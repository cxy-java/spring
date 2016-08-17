package netty.spring.exception;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * 
 * @author changxy
 * 异常统一处理
 *
 */
@Component
public class MyExceptionHandler implements HandlerExceptionResolver{

	
	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		System.out.println("=================");
		Map<String, Object> model = new HashMap<String, Object>();  
        model.put("error", ex.getMessage());  
        return new ModelAndView("exception", model);  
	}

}
