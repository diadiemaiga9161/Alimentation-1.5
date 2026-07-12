package com.ges.boutique.email;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import javax.imageio.ImageIO;

@Slf4j
@Service
public class QrCodeService {

    public byte[] genererQrCode(String contenu, int largeur, int hauteur) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, 1
            );
            BitMatrix matrix = writer.encode(contenu, BarcodeFormat.QR_CODE, largeur, hauteur, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (WriterException | java.io.IOException e) {
            log.error("Erreur generation QR code: {}", e.getMessage());
            return new byte[0];
        }
    }

    public String genererQrCodeBase64(String contenu, int largeur, int hauteur) {
        byte[] bytes = genererQrCode(contenu, largeur, hauteur);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
