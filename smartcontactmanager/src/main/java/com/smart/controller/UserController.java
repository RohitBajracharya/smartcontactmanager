package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;


import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	UserRepository userRepository;
	@Autowired
	ContactRepository contactRepository;

//	handler for sending common data to all handler
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println(userName);
//		get user using email
		User user = userRepository.getUserByUserName(userName);
		System.out.println("user " + user);
		model.addAttribute("user", user);
	}

//	handler for open user dash board
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

//	handler for open add contact
	@GetMapping("/add-contact")
	public String openAddContact(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact";
	}

//	handler for process contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("image") MultipartFile file,
			Principal principal, HttpSession session) {

		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			// processing and uploading file
			if (file.isEmpty()) {
				System.out.println("File is empty");
			} else {
				contact.setImageUrl(file.getOriginalFilename());
				File filePath = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(filePath.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Image is uploaded");
			}
			user.getContacts().add(contact);
			contact.setUser(user);

			this.userRepository.save(user);
			session.setAttribute("message", new Message("Your Contact is added", "success"));
			System.out.println("added to data base");

		} catch (Exception e) {
			System.out.println("Error " + e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong! Try again", "danger"));
		}
		return "normal/add_contact";

	}
	
	
//	handler for open view contact
//	per page = 5[n]
//	current page= 0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model model,Principal principal) {
		model.addAttribute("title","Show Contact");
//	 alternative
		/*
		 * String userName=principal.getName(); User user =
		 * this.userRepository.getUserByUserName(userName); List<Contact> contacts =
		 * user.getContacts();
		 */
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
//		current page, contact per page
		org.springframework.data.domain.Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(),pageable);
		model.addAttribute("contacts",contacts);
		model.addAttribute("currentPage",page);
		model.addAttribute("totalPages",contacts.getTotalPages());
		return "normal/show_contacts";
	}
}
