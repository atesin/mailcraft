package cl.netgamer.mailcraft;

import java.util.Map;

public class Mail
{
	// properties
	private String from;
	protected String rcpt;
	private String subj = "";
	private String mesg = "";
	private String date;
	
	// constructors
	
	// from commands
	protected Mail(String from, String rcpt, String body)
	{
		// set properties, add newlines, free resources
		this.from = from;
		this.rcpt = rcpt;
		this.date = "now";
		subj = body.replaceAll(" *\\\\", "\n").trim();
		
		// try to separate headers
		int index = subj.indexOf("\n");
		if (index >= 0)
		{
			mesg = subj.substring(index+1);
			subj = subj.substring(0, index).trim();
		}
	}
	
	// from database
	protected Mail(Map<String, Object> fields)
	{
		this((String)fields.get("mfrom"), (String)fields.get("rcpt"), (String)fields.get("subj")+"\\"+(String)fields.get("mesg"));
		this.date = (String)fields.get("date");
	}
	
	// getters + setters
	
	protected String getFrom(){ return from; }
	protected String getRcpt(){ return rcpt; }
	protected String getSubj(){ return subj; }
	protected String getMesg(){ return mesg; }
	
	// regular methods
	
	/**
	 * @return multiline text as if it were ready for sendmail
	 */
	protected String cat(){
		
		String ret = "From: "+from;
		ret += "\nTo: "+rcpt;
		ret += "\nDate: "+date;
		ret += "\nSubject: "+subj;
		ret += "\n\n"+mesg;
		return ret;
	}
}
