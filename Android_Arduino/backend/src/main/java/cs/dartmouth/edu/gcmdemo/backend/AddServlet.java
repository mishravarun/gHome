package cs.dartmouth.edu.gcmdemo.backend;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cs.dartmouth.edu.gcmdemo.backend.data.Contact;
import cs.dartmouth.edu.gcmdemo.backend.data.ContactDatastore;

public class AddServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		String device = req.getParameter("device");
		MessagingEndpoint msg = new MessagingEndpoint();
		msg.sendMessage(device);

	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		doGet(req, resp);
	}

}
