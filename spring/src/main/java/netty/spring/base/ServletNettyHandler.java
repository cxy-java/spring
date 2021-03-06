package netty.spring.base;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map.Entry;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.CharsetUtil;
import static io.netty.handler.codec.http.HttpVersion.*;

public class ServletNettyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private final Servlet servlet;

	private final ServletContext servletContext;

	public ServletNettyHandler(Servlet servlet) {
		this.servlet = servlet;
		this.servletContext = servlet.getServletConfig().getServletContext();
	}

	private MockHttpServletRequest createServletRequest(FullHttpRequest httpRequest) {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(httpRequest.uri()).build();

		MockHttpServletRequest servletRequest = new MockHttpServletRequest(this.servletContext);
		servletRequest.setRequestURI(uriComponents.getPath());
		servletRequest.setPathInfo(uriComponents.getPath());
		servletRequest.setMethod(httpRequest.method().name());

		if (uriComponents.getScheme() != null) {
			servletRequest.setScheme(uriComponents.getScheme());
		}
		if (uriComponents.getHost() != null) {
			servletRequest.setServerName(uriComponents.getHost());
		}
		if (uriComponents.getPort() != -1) {
			servletRequest.setServerPort(uriComponents.getPort());
		}

		for (String name : httpRequest.headers().names()) {
			for (String value : httpRequest.headers().getAll(name)) {
				servletRequest.addHeader(name, value);
			}
		}
		servletRequest.setContent(httpRequest.content().array());
		try {
			if (uriComponents.getQuery() != null) {
				String query = UriUtils.decode(uriComponents.getQuery(), "UTF-8");
				servletRequest.setQueryString(query);
			}

			for (Entry<String, List<String>> entry : uriComponents.getQueryParams().entrySet()) {
				for (String value : entry.getValue()) {
					servletRequest.addParameter(UriUtils.decode(entry.getKey(), "UTF-8"),
							UriUtils.decode(value, "UTF-8"));
				}
			}
		} catch (UnsupportedEncodingException ex) {
			// shouldn't happen
		}

		return servletRequest;
	}

	private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
		response.content()
				.writeBytes(Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
		ctx.write(response).addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		if (ctx.channel().isActive()) {
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof HttpRequest) {
			FullHttpRequest request = (FullHttpRequest) msg;
			if (!request.decoderResult().isSuccess()) {
				sendError(ctx, HttpResponseStatus.BAD_REQUEST);
				return;
			}
			MockHttpServletRequest servletRequest = createServletRequest(request);
			MockHttpServletResponse servletResponse = new MockHttpServletResponse();
			this.servlet.service(servletRequest, servletResponse);
			HttpResponseStatus status = HttpResponseStatus.valueOf(servletResponse.getStatus());
			HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
			for (String name : servletResponse.getHeaderNames()) {
				for (Object value : servletResponse.getHeaderValues(name)) {
					response.headers().add(name, value);
				}
			}
			// Write the initial line and the header.
			ctx.write(response);
			InputStream contentStream = new ByteArrayInputStream(servletResponse.getContentAsByteArray());
			// Write the content.
			ChannelFuture writeFuture = ctx.write(new ChunkedStream(contentStream));
			writeFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
		// TODO Auto-generated method stub
		
	}
}
