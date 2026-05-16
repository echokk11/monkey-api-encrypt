package com.unknown.encrypt.core;

import com.unknown.encrypt.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EncryptionReqestWrapper extends HttpServletRequestWrapper  {
	
	private byte[] requestBody = new byte[0];

    private final Charset charset;

    private Map<String, String> paramMap = new HashMap<>();

	public EncryptionReqestWrapper(HttpServletRequest request) {
		super(request);
        this.charset = resolveCharset(request.getCharacterEncoding());
		try {
			requestBody = StreamUtils.copyToByteArray(request.getInputStream());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    private Charset resolveCharset(String characterEncoding) {
        if (characterEncoding == null || characterEncoding.trim().length() == 0) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(characterEncoding);
    }
	
	@Override
	public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(requestBody);
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bais.read();
            }
 
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }
 
            @Override
            public boolean isReady() {
                return true;
            }
 
            @Override
            public void setReadListener(ReadListener listener) {
 
            }
        };
	}

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

	public String getRequestData() {
		return new String(requestBody, charset);
	}
	
	public void setRequestData(String requestData) {
		this.requestBody = requestData.getBytes(charset);
	}

    public void setParamMap(Map<String, String> paramMap) {
        this.paramMap = paramMap == null ? new HashMap<>() : paramMap;
    }

    @Override
    public String getParameter(String name) {
        if (paramMap.containsKey(name)) {
            return paramMap.get(name);
        }
        return super.getParameter(name);
    }

    @Override
    public String[] getParameterValues(String name) {
	    if (paramMap.containsKey(name)) {
            return new String[] { getParameter(name) };
        }
        return super.getParameterValues(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        Set<String> names = new LinkedHashSet<>();
        Enumeration<String> parameterNames = super.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            names.add(parameterNames.nextElement());
        }
        names.addAll(paramMap.keySet());
        return Collections.enumeration(names);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameterMap = new LinkedHashMap<>(super.getParameterMap());
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            parameterMap.put(entry.getKey(), new String[]{entry.getValue()});
        }
        return Collections.unmodifiableMap(parameterMap);
    }
}
