package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

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
				contact.setImageUrl("default.png");
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
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "Show Contact");
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
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());
		return "normal/show_contacts";
	}

//	handler to show particular contact details
	@GetMapping("/{cid}/contact")
	public String showContactDetails(@PathVariable("cid") Integer cid, Model model, Principal principal) {
		Optional<Contact> contactOptional = this.contactRepository.findById(cid);
		Contact contact = contactOptional.get();
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		return "normal/contact_details";
	}

//	handler to delete contact
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cid, Principal principal, HttpSession session) {
		Contact contact = this.contactRepository.findById(cid).get();
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		if (user.getId() == contact.getUser().getId()) {
			/*
			 * contact.setUser(null); this.contactRepository.delete(contact);
			 */
			user.getContacts().remove(contact);
			this.userRepository.save(user);
			session.setAttribute("message", new Message("Contact Deleted Successfully", "success"));
		}
		return "redirect:/user/show-contacts/0";
	}

//	handler to show edit contact form
	@PostMapping("/edit-contact/{cid}")
	public String editContact(@PathVariable("cid") Integer cid, Model model) {
		model.addAttribute("title", "Edit Contact");
		Contact contact = this.contactRepository.findById(cid).get();
		model.addAttribute("contact", contact);
		return "normal/edit_contact";
	}

//	handler to update contact form
	@PostMapping("/update-contact")
	public String updateContact(@ModelAttribute Contact contact, @RequestParam("image") MultipartFile file, Model model,
			HttpSession session, Principal principal) {
		try {
			//old contact details
			Contact oldContactDetails = this.contactRepository.findById(contact.getCid()).get();
			//image
			if (!file.isEmpty()) {
				// delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile,oldContactDetails.getImageUrl());
				file1.delete();
				//update new photo				
				File filePath = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(filePath.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImageUrl(file.getOriginalFilename());				
			}else {
				contact.setImageUrl(oldContactDetails.getImageUrl());
			}
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			session.setAttribute("message", new Message("Contact Successfully Edited", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "redirect:/user/"+contact.getCid()+"/contact";
	}
	
//	handler to display your profile
	@GetMapping("/profile")
	public String displayProfile(Model model) {
		
		model.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	
	
	
}
