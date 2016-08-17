package netty.spring.base;

import javax.servlet.ServletException;

import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

public class DispatcherServletChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final DispatcherServlet dispatcherServlet;
	
	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		ChannelPipeline pipeline = channel.pipeline();
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		pipeline.addLast("handler", new ServletNettyHandler(this.dispatcherServlet));
	}

	public DispatcherServletChannelInitializer() throws ServletException {

    	MockServletContext servletContext = new MockServletContext();
    	MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("contextConfigLocation","classpath:/META-INF/spring/root-context.xml");
        servletContext.addInitParameter("contextConfigLocation","classpath:/META-INF/spring/root-context.xml");
    	//AnnotationConfigWebApplicationContext wac = new AnnotationConfigWebApplicationContext();
        XmlWebApplicationContext wac = new XmlWebApplicationContext();
        //ClassPathXmlApplicationContext wac = new ClassPathXmlApplicationContext();
		wac.setServletContext(servletContext);
		wac.setServletConfig(servletConfig);
        wac.setConfigLocation("classpath:/servlet-context.xml");
    	//wac.register(WebConfig.class);
    	wac.refresh();
    	this.dispatcherServlet = new DispatcherServlet(wac);
    	this.dispatcherServlet.init(servletConfig);
	}
}
