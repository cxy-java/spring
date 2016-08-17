package netty.spring.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloController {

	@RequestMapping("/hello")
	@ResponseBody
	public Map<String, Object> hello(@RequestParam(value = "name", required = false) String name) throws Exception {
		System.out.println("name=" + name);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("msg", "success");
		map.put("data", null);
		if (name == null) {
			throw new Exception("name not null");
		}
		return map;
	}
}
