package com.socket.server.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 验证码生成器
 */
public class Captcha {
    private final char[] codeSequence = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private final int codeCount, lineCount;
    private final int width, height;
    private String code;

    /**
     * 配置图形验证码
     *
     * @param width     图片宽
     * @param height    图片高
     * @param codeCount 字符个数
     * @param lineCount 干扰线条数
     */
    public Captcha(int width, int height, int codeCount, int lineCount) {
        this.width = width;
        this.height = height;
        this.codeCount = codeCount;
        this.lineCount = lineCount;
    }

    /**
     * 生成图形验证码
     */
    public BufferedImage getBuffImage() {
        int x = width / (codeCount + 2), fontHeight = height - 2, codeY = height - 4;
        int red, green, blue;
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, width, height);
        Font font = new Font(null, Font.PLAIN, fontHeight);
        Random random = new Random();
        g2d.setFont(font);
        for (int i = 0; i < lineCount; i++) {
            int xs = random.nextInt(width);
            int ys = random.nextInt(height);
            int xe = xs + random.nextInt(width / 8);
            int ye = ys + random.nextInt(height / 8);
            red = random.nextInt(255);
            green = random.nextInt(255);
            blue = random.nextInt(255);
            g2d.setColor(new Color(red, green, blue));
            g2d.drawLine(xs, ys, xe, ye);
        }
        StringBuilder randomCode = new StringBuilder();
        for (int i = 0; i < codeCount; i++) {
            String strRand = String.valueOf(codeSequence[random.nextInt(codeSequence.length)]);
            red = random.nextInt(255);
            green = random.nextInt(255);
            blue = random.nextInt(255);
            g2d.setColor(new Color(red, green, blue));
            g2d.drawString(strRand, (i + 1) * x, codeY);
            randomCode.append(strRand);
        }
        code = randomCode.toString();
        return bufferedImage;
    }

    public String getCode() {
        return code.toLowerCase();
    }
}
