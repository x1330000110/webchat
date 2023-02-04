package com.socket.secure.core.generator;

/**
 * 签名生成器
 */
public interface SignatureGenerator {
    /**
     * 生成公钥签名
     *
     * @param bytes 公钥数据
     * @return 公钥签名
     */
    String generatePublicKeySignature(byte[] bytes);

    /**
     * 生成文件名签名
     *
     * @param filename 文件名
     * @return 文件名签名
     */
    String generateFileNameSignature(String filename);
}
