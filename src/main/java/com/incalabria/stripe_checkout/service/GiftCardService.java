package com.incalabria.stripe_checkout.service;

import com.incalabria.stripe_checkout.entity.GiftCard;
import com.incalabria.stripe_checkout.enumeration.GiftCardType;
import com.incalabria.stripe_checkout.repository.GiftCardRepository;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class GiftCardService {
    @Autowired
    private GiftCardRepository repository;

    public GiftCard createGiftCard(GiftCard giftCard) {
        String code;
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            code = generateRandomCode(10);
            if (!repository.existsByCode(code)) {
                giftCard.setCode(code);
                return repository.save(giftCard);
            }
        }
        // Se non ha trovato un codice unico dopo maxAttempts, lancia eccezione
        throw new IllegalStateException("Impossibile generare un codice giftcard unico");
    }

    public byte[] generateGiftCardImage(
            GiftCardType type,
            String receiver,
            String giftCardId,
            String message,
            String sender) throws IOException {

        String html = buildHtml(type, receiver, giftCardId, message, sender);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(950, 1100)
            );
            Page page = context.newPage();
            page.setContent(html);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true)
                    .setType(ScreenshotType.PNG)
            );

            context.close();
            browser.close();

            return screenshot;
        }
    }

    private String buildHtml(GiftCardType type, String receiver, String giftCardId,
                             String message, String sender) {
        String textShadow = "";
        String textStyle = "color: " + type.getTextBackgroundColor() + ";";

        if (type.hasInnerShadow()) {
            textShadow = "text-shadow: 0px 4px 4px " + type.getShadowColor() + ", 0 0 0 " + type.getTextBackgroundColor() + ";";
            textStyle = "color: rgba(0,0,0,0);";
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gift Card</title>
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@300;400;600&display=swap" rel="stylesheet">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Montserrat', sans-serif;
            display: flex;
            flex-direction: column;
            gap: 50px;
            padding: 50px;
            align-items: center;
            justify-content: center;
        }

        .gift-card-container {
            width: 850px;
            height: 550px;
            border-radius: 30px;
            background: linear-gradient(112.12deg, %s 3%%, %s 18%%, %s 37%%, %s 55%%, %s 77%%, %s 100%%);
            position: relative;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            padding: 50px;
            page-break-inside: avoid;
        }

        .gift-card-front {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            gap: 30px;
            height: 100%%;
        }

        .receiver-name {
            font-size: 25px;
            font-weight: 400;
            %s
            %s
            text-align: center;
        }

        .logo-container {
            display: flex;
            flex-direction: row;
            align-items: center;
            gap: 15px;
            height: 89.84px;
        }

        .logo-image {
            height: 89.84px;
            width: auto;
        }

        .id-text {
            font-size: 25px;
            font-weight: 400;
            %s
            %s
            text-align: center;
        }

        .id-label {
            font-weight: 600;
        }

        .id-value {
            font-weight: 400;
        }

        .amount {
            position: absolute;
            top: 60px;
            right: 50px;
            font-size: 50px;
            font-weight: 400;
            %s
            %s
            text-align: right;
        }

        .website {
            position: absolute;
            bottom: 50px;
            left: 50%%;
            transform: translateX(-50%%);
            font-size: 25px;
            font-weight: 400;
            %s
            %s
            text-align: center;
        }

        .gift-card-back {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            gap: 0;
            height: 100%%;
            width: 100%%;
            padding: 150px;
        }

        .back-content {
            display: flex;
            flex-direction: column;
            gap: 50px;
            align-items: stretch;
            width: 100%%;
            height: 100%%;
        }

        .message {
            font-size: 25px;
            font-weight: 400;
            line-height: 150%%;
            %s
            %s
            text-align: left;
            width: 100%%;
        }

        .sender-name {
            font-size: 25px;
            font-weight: 400;
            %s
            %s
            text-align: right;
            width: 100%%;
        }

        @media print {
            body {
                padding: 0;
                gap: 0;
            }
            .gift-card-container {
                page-break-after: always;
                margin: 0;
            }
        }
    </style>
</head>
<body>
    <div class="gift-card-container">
        <div class="amount">%s€</div>
        
        <div class="gift-card-front">
            <div class="receiver-name">%s</div>
            
            <div class="logo-container">
                <img src="%s" alt="InCalabria Logo" class="logo-image">
            </div>
            
            <div class="id-text"><span class="id-label">id:</span> <span class="id-value">%s</span></div>
        </div>
        
        <div class="website">www.incalabria.net</div>
    </div>

    <div class="gift-card-container">
        <div class="gift-card-back">
            <div class="back-content">
                <div class="message">%s</div>
                <div class="sender-name">%s</div>
            </div>
        </div>
    </div>
</body>
</html>
                """.formatted(
                type.getGradientColor1(), type.getGradientColor2(), type.getGradientColor3(),
                type.getGradientColor4(), type.getGradientColor5(), type.getGradientColor6(),
                textStyle, textShadow,
                textStyle, textShadow,
                textStyle, textShadow,
                textStyle, textShadow,
                textStyle, textShadow,
                textStyle, textShadow,
                type.getAmount(), receiver, type.getLogoUrl(), giftCardId, message, sender
        );
    }

    private String generateRandomCode(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public Double withdrawFromGiftCard(String code, double amount) {
        Optional<GiftCard> giftCard = repository.findByCode(code);
        if (giftCard.isPresent()) {
            double initialAmount = giftCard.get().getAmount();
            double leftAmount = Math.max(0, initialAmount - amount);
            giftCard.get().setAmount(leftAmount);
            repository.save(giftCard.get());
            return initialAmount - leftAmount;
        }
        return 0.0;
    }

}
