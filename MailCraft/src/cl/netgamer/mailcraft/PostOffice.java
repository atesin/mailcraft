package cl.netgamer.mailcraft;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PostOffice
{
	private Database db;
	private Map<String, Mail> lastViewed = new HashMap<String, Mail>();
	
	// CONSTRUCTOR
	
	public PostOffice(Database db)
	{
		this.db = db;
		
		// create tables and console user mailbox if not exist
		String query = "CREATE TABLE IF NOT EXISTS mail_box (";
		query += "id TEXT NOT NULL,";
		query += "owner TEXT NOT NULL,";
		query += "PRIMARY KEY (id(20)));";
		this.db.set(query);
		
		query  = "CREATE TABLE IF NOT EXISTS mail_msg (";
		query += "id int(10) unsigned NOT NULL AUTO_INCREMENT,";
		query += "mbox text NOT NULL,";
		query += "mread tinyint(4) NOT NULL,";
		query += "mfrom text NOT NULL,";
		query += "rcpt text NOT NULL,";
		query += "date text NOT NULL,";
		query += "subj text NOT NULL,";
		query += "mesg text NOT NULL,";
		query += "PRIMARY KEY (id));";
		this.db.set(query);
		
		// create console admin mailbox
		mailboxCreate("CONSOLE");
		
		MC.log("Database tables created (if not exist)");
	}
	
	// mailbox operations
	
	// admin: list mailboxes
	protected String mailboxList(String owner)
	{
		String list = "";
		for (Object name: db.getCol("SELECT DISTINCT owner FROM mail_box WHERE owner LIKE '%"+owner+"%';"))
			list += (list.length() == 0?"":", ")+(String)name;
		return list;
	}
	
	// admin: create mailbox
	protected int mailboxCreate(String owner)
	{
		// check if already exists
		// to avoid overwrite or duplication
		if ((long)db.getData("SELECT COUNT(id) FROM mail_box WHERE owner='"+owner+"';") != 0) return 553;
		
		db.set("INSERT IGNORE mail_box VALUES ('"+owner+":inbox', '"+owner+"');");
		db.set("INSERT IGNORE mail_box VALUES ('"+owner+":sent', '"+owner+"');");
		db.set("INSERT IGNORE mail_box VALUES ('"+owner+":trash', '"+owner+"');");
		
		// welcome message
		String b = "Welcome\\";
		b += "Welcome to our server, please check the rules at our forum.\\";
		b += "Get more info on mail system by typing /mail help";
		send(new Mail("MailCraft", owner, b));
		return 250;
	}
	
	// admin: delete mailbox
	protected void mailboxDelete(String owner)
	{
		// also delete messages
		db.set("DELETE IGNORE FROM mail_msg WHERE mbox='"+owner+":inbox';");
		db.set("DELETE IGNORE FROM mail_msg WHERE mbox='"+owner+":sent';");
		db.set("DELETE IGNORE FROM mail_msg WHERE mbox='"+owner+":trash';");
		db.set("DELETE IGNORE FROM mail_box WHERE owner='"+owner+"';");
	}
	
	// admin: rename mailbox
	protected int mailboxRename(String curOwner, String newOwner)
	{
		// check if mailbox already exists, to avoid overwrite or duplication
		// doesnt check if new player EXISTS so type it carefully (case sensitive)
		if ((long)db.getData("SELECT COUNT(id) FROM mail_box WHERE owner='"+newOwner+"';") != 0) return 553;
		
		db.set("UPDATE IGNORE mail_box SET id='"+newOwner+":inbox', owner='"+newOwner+"' WHERE id='"+newOwner+":inbox';");
		db.set("UPDATE IGNORE mail_box SET id='"+newOwner+":sent', owner='"+newOwner+"' WHERE id='"+newOwner+":sent';");
		db.set("UPDATE IGNORE mail_box SET id='"+newOwner+":trash', owner='"+newOwner+"' WHERE id='"+newOwner+":trash';");
		db.set("UPDATE IGNORE mail_msg SET mbox='"+newOwner+":inbox' WHERE mbox='"+curOwner+":inbox';");
		db.set("UPDATE IGNORE mail_msg SET mbox='"+newOwner+":sent' WHERE mbox='"+curOwner+":sent';");
		db.set("UPDATE IGNORE mail_msg SET mbox='"+newOwner+":trash' WHERE mbox='"+curOwner+":trash';");
		return 250;
	}
	
	// user methods
	
	/**
	 * retrieve mail listfrom folder
	 * no pages or order for now
	 * @param owner
	 * @param folder
	 * @return
	 */
	protected String list(String owner, String folder)
	{
		// retrieve folder contents, create if not exist
		// get folder id first, PLEASE NOTE THIS RELY ON COMMANDS
		mailboxCreate(owner);
		//double mbox = getMailbox(owner, folder);
		
		// get folder contents
		String list = "id / mread / mfrom / rcpt / date / subj";
		for (HashMap<String, Object> row: db.getTable("SELECT * FROM mail_msg WHERE mbox='"+owner+":"+folder+"' ORDER BY id DESC;"))
			list += "\n"+row.get("id")+" / "+row.get("mread")+" / "+row.get("mfrom")+" / "+row.get("rcpt")+" / "+row.get("date")+" / "+row.get("subj");
		return list;
	}
	
	/**
	 * recieve and send mail
	 */
	protected void send(Mail mail)
	{
		// check names first
		// get mailbox owners
		Map<String, String> owners = new HashMap<String, String>();
		for (Object o: db.getCol("SELECT DISTINCT owner FROM mail_box;"))
			owners.put(((String)o).toUpperCase(), (String)o);
		
		// update clean recipient lists
		String avail = "";
		String unavail = "";
		String r = "";
		for (String to: mail.rcpt.split(","))
		{
			if (owners.containsKey(to.toUpperCase()))
			{
				to = (String)owners.get(to.toUpperCase());
				avail += (avail.length() == 0?"":",")+to;
			}
			else
				unavail += (unavail.length() == 0?"":",")+to;
			r += (r.length() == 0?"":",")+to;
		}
		mail.rcpt = r;
		
		// id mbox mread mfrom rcpt date subj mesg
		
		// store in sent folder
		// get sent folder first, create if not exist
		// no reply messages
		if (!mail.getFrom().equals("MailCraft"))
		{
			mailboxCreate(mail.getFrom());
			db.set("INSERT mail_msg VALUES (null, '"+mail.getFrom()+":sent', 0, '"+mail.getFrom()+"', '"+r+"', 'now', '"+mail.getSubj()+"', '"+mail.getMesg()+"');");
		}
		
		// deliver and notify
		for (String to: avail.split(","))
		{
			db.set("INSERT mail_msg VALUES (null, '"+to+":inbox', 0, '"+mail.getFrom()+"', '"+r+"', 'now', '"+mail.getSubj()+"', '"+mail.getMesg()+"');");
			if (to.equals("CONSOLE"))
				MC.log("You have new mail!");
			else
			{
				Player p = Bukkit.getPlayerExact(to);
				if (p != null)
					p.sendMessage("§EYou have new mail!");
			}
		}
		
		// no errors: exit
		if (unavail.length() == 0)
			return;
		
		// send error message
		avail  = "This message was created automatically by MailCraft\n";
		avail += "Delivery to the following recipients failed permanently:\n - ";
		avail += unavail;
		avail += "\nThe error returned was: 550 mailbox unavailable";
		db.set("INSERT mail_msg VALUES (null, '"+mail.getFrom()+":inbox', 0, 'MailCraft', '"+r+"', 'Delivery failure notification', 'now', '"+avail+"');");
		if (mail.getFrom().equals("CONSOLE"))
			MC.log("You have new mail!");
		else
			Bukkit.getPlayerExact(mail.getFrom()).sendMessage("§EYou have new mail!");
	}
	
	/**
	 * try to display mail message if you can
	 */
	protected String retrieve(String owner, int id)
	{
		// try to get mail
		Map<String, Object> m = db.getRow("SELECT * FROM mail_msg WHERE id="+id+";");
		if (m == null)
			return null;
		
		// is yours?
		if (!((String)m.get("mbox")).matches("^"+owner+":.*"))
			return null;
		
		Mail mail = new Mail(m);
		
		// mark as read and remember
		db.set("UPDATE mail_msg SET mread=1 WHERE id="+id+";");
		lastViewed.put(owner, mail);
		
		// return mail
		return mail.cat();
	}
	
	/**
	 * delete: move to trash... easy, just change folder
	 */
	protected void recycle(String owner, String[] id)
	{
		String ids = "";
		for (String i: id)
		{
			// looks like integer?
			try
			{
				ids += (ids.length() == 0?"":",")+Integer.parseInt(i);
			}
			catch (NumberFormatException e) {continue;}
			
		// delete messages owner
		db.set("UPDATE IGNORE mail_msg SET mbox='"+owner+":trash' WHERE id IN ("+ids+") AND mbox LIKE '"+owner+":%';");
		}
		
		// pending: delete lasts and always leave 10
	}
	
	/**
	 * uses remembered last viewed mail
	 * @return 250: ok, 451: no previously viewed mail
	 */
	protected int reply(String owner, String subj, String[] mesg)
	{
		// have you readed some mail before?
		Mail mail = lastViewed.get(owner);
		if (mail == null) { return 451; }
		
		// fix message
		String m = "";
		if (mesg != null)
		{
			for (String b: mesg)
				m += b+" ";
			m += "\\";
		}

		// fix subject
		String s = mail.getSubj().replaceAll("^("+subj+")*", subj);
		
		// send it
		send(new Mail(owner, mail.getFrom(), s+"\\"+m+"#\\"+mail.cat()));
		return 250;
	}
	
	/**
	 * uses remembered last viewed mail
	 * @return 250: ok, 451: no previously viewed mail
	 */
	protected int replyAll(String owner, String subj, String[] mesg)
	{
		// have you readed some mail before?
		Mail mail = lastViewed.get(owner);
		if (mail == null) { return 451; }
		
		// fix message
		String m = "";
		if (mesg != null)
		{
			for (String b: mesg)
				m += b+" ";
			m += "\\";
		}

		// fix recipients
		String rcpt = (mail.getFrom()+","+mail.getRcpt()).replaceAll("(^|,)"+owner+"(,|$)", "$1$2").replaceAll(",,", ",");

		// fix subject
		String s = mail.getSubj().replaceAll("^("+subj+")*", subj);
		
		// send it
		send(new Mail(owner, rcpt, s+"\\"+m+"#\\"+mail.cat()));
		return 250;
	}
	
	protected int forward(String owner, String to, String subj, String[] mesg)
	{
		// have you readed some mail before?
		Mail mail = lastViewed.get(owner);
		if (mail == null) { return 451; }
		
		// fix message
		String m = "";
		if (mesg != null)
		{
			for (String b: mesg)
				m += b+" ";
			m += "\\";
		}

		// fix subject
		String s = mail.getSubj().replaceAll("^("+subj+")*", subj);
		
		// send it
		send(new Mail(owner, to, s+"\\"+m+"#\\"+mail.cat()));
		return 250;
	}
}
