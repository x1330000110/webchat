package com.socket.secure.filter;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.socket.secure.constant.SecureConstant;
import com.socket.secure.exception.InvalidRequestException;
import com.socket.secure.util.AES;
import com.socket.secure.util.Hmac;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Secure servlet request wrapper
 */
final class SecureRequestWrapper extends HttpServletRequestWrapper {
    /**
     * Json config
     */
    private final JSONConfig config = JSONConfig.create().setOrder(true).setIgnoreNullValue(false);
    /**
     * Client standard form data
     */
    private final Map<String, String[]> params;
    /**
     * Client request body data
     */
    private final JSONObject body;
    /**
     * Client request file data
     */
    private final Collection<Part> part;
    /**
     * Link timestamp
     */
    private String timestamp36;
    /**
     * Link signature
     */
    private String signature;

    /**
     * Secure servlet request wrapper
     *
     * @param request {@link ServletRequest}
     * @throws IllegalArgumentException If the request parameter is empty
     */
    public SecureRequestWrapper(HttpServletRequest request) throws IOException, ServletException {
        super(request);
        this.params = new LinkedHashMap<>(super.getParameterMap());
        this.part = this.isFileRequest() ? super.getParts() : Collections.emptyList();
        this.body = this.getBody();
    }

    /**
     * If From Data is empty and Parts is empty, try to get body content
     */
    private JSONObject getBody() throws IOException {
        if (!isFileRequest()) {
            String obj = super.getReader().lines().collect(Collectors.joining());
            if (JSONUtil.isJson(obj)) {
                return JSONUtil.parseObj(obj, config);
            }
        }
        return new JSONObject();
    }

    /**
     * Check if this request include file
     */
    private boolean isFileRequest() {
        String type = getContentType();
        return StrUtil.isNotEmpty(type) && type.startsWith(ContentType.MULTIPART.getValue());
    }

    @Override
    public String getParameter(String name) {
        String[] arr = params.get(name);
        return arr == null || arr.length == 0 ? null : arr[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(params);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(params.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return params.get(name);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new ServletInputStream() {
            private final ByteBuffer buffer = ByteBuffer.wrap(body.toString().getBytes());

            @Override
            public boolean isFinished() {
                return buffer.remaining() == 0;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return isFinished() ? -1 : buffer.get();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        ServletInputStream stream = getInputStream();
        InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
        return new BufferedReader(isr);
    }

    /**
     * try to decrypt all data
     *
     * @param signName request signature name
     * @throws InvalidRequestException decryption error
     */
    void decryptRequset(String signName) {
        // Decrypt FormData
        params.forEach(this::decrypt);
        // Decryption request body
        body.forEach(this::decrypt);
        // Find signature
        String signature = findSignature(signName);
        this.signature = signature.substring(0, 40);
        this.timestamp36 = signature.substring(40);
    }

    /**
     * Decrypt FormData
     */
    private void decrypt(String key, String[] values) {
        if (isValid(values)) {
            String val = values[0] = AES.decrypt(values[0], getSession());
            // Find array tag after decryption
            String[] vals = val.contains(SecureConstant.ARRAY_MARK) ? val.split(SecureConstant.ARRAY_MARK) : values;
            params.put(key, vals);
        }
    }

    /**
     * Decryption request body
     */
    private void decrypt(String key, Object value) {
        if (isValid(value)) {
            String val = AES.decrypt((String) value, getSession());
            // Find array tag after decryption
            Object obj = val.contains(SecureConstant.ARRAY_MARK) ? val.split(SecureConstant.ARRAY_MARK) : val;
            body.set(key, obj);
        }
    }

    /**
     * Checks whether the specified object is a string or a string array with only one element
     */
    private boolean isValid(Object obj) {
        return obj instanceof String || obj instanceof String[] && ((String[]) obj).length == 1;
    }

    /**
     * Find request signature
     */
    private String findSignature(String sign) {
        // find signatures in forms
        String[] values = params.remove(sign);
        if (values != null && values.length == 1) {
            return values[0];
        }
        // find the signature in the payload
        if (body.containsKey(sign)) {
            return (String) body.remove(sign);
        }
        // Can't find throws exception
        throw new InvalidRequestException("Signature is Null");
    }

    /**
     * Verify that the signature matches the data
     *
     * @param vaildPart Whether to verify the file
     * @return match returns true
     */
    boolean matchSignature(boolean vaildPart) throws IOException {
        // Generate signature
        StringBuilder sb = new StringBuilder();
        // join form data
        for (String[] value : params.values()) {
            if (value.length == 0) {
                continue;
            }
            // handle array
            if (value.length > 1) {
                sb.append(String.join(SecureConstant.ARRAY_MARK, value));
                continue;
            }
            sb.append(value[0]);
        }
        // join body
        for (Object value : body.values()) {
            // handle array
            if (value.getClass().isArray()) {
                String[] strings = Arrays.stream((Object[]) value).map(Object::toString).toArray(String[]::new);
                sb.append(String.join(SecureConstant.ARRAY_MARK, strings));
                continue;
            }
            sb.append(value);
        }
        // join part diest
        if (vaildPart) {
            for (Part part : part) {
                String fileName = part.getSubmittedFileName();
                if (fileName != null) {
                    String digest = Hmac.MD5.digestHex(getSession(), IoUtil.readBytes(part.getInputStream()));
                    sb.append(Hmac.MD5.digestHex(getSession(), fileName + digest));
                }
            }
        }
        // check sign
        String digest = Hmac.SHA1.digestHex(getSession(), sb + timestamp36);
        return digest.equalsIgnoreCase(signature);
    }

    /**
     * request timestamp
     */
    long getTimestamp() {
        return Long.parseLong(timestamp36, 36);
    }

    /**
     * request signature
     */
    String sign() {
        return signature;
    }
}
