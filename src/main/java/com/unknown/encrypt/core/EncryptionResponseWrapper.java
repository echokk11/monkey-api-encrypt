package com.unknown.encrypt.core;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class EncryptionResponseWrapper extends HttpServletResponseWrapper {

	private ServletOutputStream filterOutput;

	private PrintWriter writer;

	private final ByteArrayOutputStream output;

	private final Charset charset;

	public EncryptionResponseWrapper(HttpServletResponse response) {
		super(response);
		output = new ByteArrayOutputStream();
		charset = resolveCharset(response.getCharacterEncoding());
	}

	private Charset resolveCharset(String characterEncoding) {
		if (characterEncoding == null || characterEncoding.trim().length() == 0) {
			return StandardCharsets.UTF_8;
		}
		return Charset.forName(characterEncoding);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (writer != null) {
			throw new IllegalStateException("getWriter() has already been called");
		}
		if (filterOutput == null) {
			filterOutput = new ServletOutputStream() {
				@Override
				public void write(int b) throws IOException {
					output.write(b);
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setWriteListener(WriteListener writeListener) {
				}
			};
		}

		return filterOutput;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (filterOutput != null) {
			throw new IllegalStateException("getOutputStream() has already been called");
		}
		if (writer == null) {
			writer = new PrintWriter(new OutputStreamWriter(output, charset));
		}
		return writer;
	}

	@Override
	public void flushBuffer() throws IOException {
		if (writer != null) {
			writer.flush();
		}
		if (filterOutput != null) {
			filterOutput.flush();
		}
	}

	public String getResponseData() {
		if (writer != null) {
			writer.flush();
		}
		return new String(output.toByteArray(), charset);
	}

	public byte[] getResponseBytes() {
		if (writer != null) {
			writer.flush();
		}
		return output.toByteArray();
	}

}
