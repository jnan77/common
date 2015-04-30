package org.son.chat.common.net.core.coder.impl;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

import org.son.chat.common.net.core.coder.ICoder;
import org.son.chat.common.net.core.coder.ICoderParserManager;
import org.son.chat.common.net.core.coder.IHandle;
import org.son.chat.common.net.core.coder.IPackageCoder;
import org.son.chat.common.net.core.coder.IcoderCtx;
import org.son.chat.common.net.exception.CoderException;

/**
 * @author solq
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class CoderParserManager implements ICoderParserManager {

	private LinkedHashMap<String, CoderParser> coderParsers = new LinkedHashMap<>();

	@Override
	public CoderResult decode(ByteBuffer buffer,IcoderCtx ctx) {
 
		for (CoderParser coderParser : coderParsers.values()) {
			final IPackageCoder packageCoder = coderParser.getPackageCoder();
			final ICoder<?, ?>[] linkCoders = coderParser.getCoders();
			final IHandle handle = coderParser.getHandle();

			// 包协议处理
			if (!packageCoder.verify(buffer,ctx)) {
				continue;
			}
			// 包解析
			Object value = packageCoder.decode(buffer,ctx);
			if (value == null) {
				// 包未读完整
				return CoderResult.UNFINISHED();
			}
			// 链式处理
			if (linkCoders != null) {
				for (ICoder coder : linkCoders) {
					value = coder.decode(value,ctx);
					if (value == null) {
						throw new CoderException("解码出错 : " + coder.getClass());
					}
				}
			}
			// 业务解码处理
			value = handle.decode(value,ctx);
			handle.handle(value,ctx);
			return CoderResult.SUCCEED(); 
		}
		throw new CoderException("未找到编/解码处理器 ");
	}

	@Override
	public ByteBuffer encode(Object message,IcoderCtx ctx) {

		for (CoderParser coderParser : coderParsers.values()) {
			final IPackageCoder packageCoder = coderParser.getPackageCoder();
			final ICoder<?, ?>[] linkCoders = coderParser.getCoders();
			final IHandle handle = coderParser.getHandle();
			// 业务检查
			if (!handle.verify(message,ctx)) {
				continue;
			}
			// 业务编码处理
			Object value = handle.encode(message,ctx);
			// 链式处理
			if (linkCoders != null) {
				for (int i = linkCoders.length - 1; i >= 0; i--) {
					ICoder coder = linkCoders[i];
					value = coder.encode(value,ctx);
					if (value == null) {
						throw new CoderException("编码出错 : " + coder.getClass());
					}
				}
			}
			// 打包消息处理
			value = packageCoder.encode(value,ctx);
			if (value != null) {
				return (ByteBuffer)value;
			}
			throw new CoderException("编码出错  :" + packageCoder.getClass());
		}

		throw new CoderException("未找到编/解码处理器 ");
	}

	@Override
	public void error(ByteBuffer buffer,IcoderCtx ctx) {

	}

	@Override
	public synchronized void register(CoderParser coderParser) {
		if (coderParsers.containsKey(coderParser.getName())) {
			throw new CoderException("已注册编/解码处理器 : " + coderParser.getName());
		}
		coderParsers.put(coderParser.getName(), coderParser);
	}

}