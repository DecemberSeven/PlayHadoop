package com.play.cn.controller;

import com.play.cn.test.AppTest;
import com.play.cn.test.TestCommunication1;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import javax.servlet.http.HttpServletRequest;

@Controller
@Scope(value="prototype")
@RequestMapping("container")
public class ContainerController extends BaseController {

	/**
	 * @return
	 */
	@RequestMapping(value="index",method= RequestMethod.GET)
	public String index(HttpServletRequest request, Model model){
		resultMap.put("title", "Continer列表");
		resultMap.put("navigation_url", "/container/index.shtml");
		resultMap.put("navigation_title", "Yarn中心");
		resultMap.put("navigation_sub_title", "Container列表");
		model.addAttribute("resultMap", resultMap);
		TestCommunication1 instance = AppTest.getTestCommunication1();
		model.addAttribute("containerInfoQueue", instance.getRunningQueue());

		// TODO 使用下面正式环境信息，需要修改对应的页面内容
//		ApplicationMaster am = App.getAm();
//		model.addAttribute("containerInfoQueue", am.getRunningContainers());

		return "container/index";
	}

}
