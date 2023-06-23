package com.smart.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	UserRepository userRepository;
	
//	handler for sending common data to all handler
	@ModelAttribute
	public void addCommonData(Model model,Principal principal) {
		String userName = principal.getName();
		System.out.println(userName);
//		get user using email
		User user = userRepository.getUserByUserName(userName);
		System.out.println("user "+user);
		model.addAttribute("user",user);
	}
	
//	handler for open user dash board
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		model.addAttribute("title","User Dashboard");
		return "normal/user_dashboard";
	}
	
	
//	handler for open add contact
	@GetMapping("/add-contact")
	public String openAddContact(Model model) {
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact";
	}
	
//	handler for process contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,Principal principal) {
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		contact.setUser(user);
		user.getContacts().add(contact);
		this.userRepository.save(user);
		System.out.println("added to data base");
		return "normal/add_contact";
	}
}





































