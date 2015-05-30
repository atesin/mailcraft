package cl.netgamer.mailcraft;

import java.util.Arrays;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Commands implements CommandExecutor
{
	private PostOffice po;
	
	/** CONSTRUCTOR */
	
	public Commands(Database db)
	{
		po = new PostOffice(db);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args)
	{
		// previous checks
		
		if (!(cmd.getName().equalsIgnoreCase("mailcraft")))
		{
			// not "mailcraft" command
			return true;
		}
		
		// default command: show inbox page 1
		
		if (args.length == 0) args = new String[] {"inbox"};
		
		// subcommand aliases
		
		switch (args[0].toLowerCase())
		{
		case "m":
		case "mb":
			args[0] = "mailbox";
			break;	
		case "i":
		case "in":
			args[0] = "inbox";
			break;
		case "s":
		case "compose":
		case "c":
			args[0] = "send";
			break;
		case "d":
		case "del":
			args[0] = "delete";
			break;
		case "r":
		case "re":
			args[0] = "reply";
		case "ra":
			args[0] = "replyall";
			break;
		case "f":
		case "fw":
			args[0] = "forward";
			break;
		case "h":
		case "?":
			args[0] = "help";
		}
		
		// parse + ejecute
		
		int status = 250;
		switch (args[0].toLowerCase())
		{
		case "mailbox":
			if (!MC.can(sender, "mailcraft.admin"))
				status = 530;
			else if (args.length == 3)
				switch (args[1].toLowerCase())
				{
				case "create":
				case "c":
					if (args.length != 3)
						status = 501;
					else if (!args[2].matches("^[a-zA-Z0-9_]{3,16}$"))
						status = 501;
					else
						status = po.mailboxCreate(args[2]);
					break;
				case "list":
				case "l":
					if (args.length != 3)
						status = 501;
					else if (!args[2].matches("^[a-zA-Z0-9_%]{1,16}$"))
						status = 501;
					else
						sender.sendMessage(po.mailboxList(args[2]));
					break;
				case "delete":
				case "del":
				case "d":
					if (args.length != 3)
						status = 501;
					else if (!args[2].matches("^[a-zA-Z0-9_]{3,16}$"))
						status = 501;
					else
						po.mailboxDelete(args[2]);
				default:
					status = 501;
				}
			else if (args.length != 4)
				status = 501;
			else
				switch (args[1].toLowerCase())
				{
				case "rename":
				case "ren":
				case "r":
					if (args.length != 4)
						status = 501;
					else if (!args[2].matches("^[a-zA-Z0-9_]{3,16}$") || !args[3].matches("^[a-zA-Z0-9_]{3,16}$"))
						status = 501;
					else
						status = po.mailboxRename(args[2], args[3]);
					break;
				default:
					status = 501;
				}
			break;
		case "inbox":
		case "sent":
		case "trash":
			// no pages for now
			// if null there is no such folder
			if (args.length > 1)
				status = 501;
			else
			{
				//String list = po.list(sender.getName(), args[0].toLowerCase());
				for (String l: po.list(sender.getName(), args[0].toLowerCase()).split("\n"))
					sender.sendMessage(l);
			}
			break;
		case "send":
			// validate recipients list
			if (!args[1].matches("^[a-zA-Z0-9_]{3,16}(,[a-zA-Z0-9_]{3,16})*$"))
			{
				status = 501;
				break;
			}
			
			// cleanse body
			String body = "";
			if (args.length > 2)
				body = MC.cleanseBody(Arrays.copyOfRange(args, 2, args.length));
			
			// looks ok
			po.send(new Mail(sender.getName(), args[1], body));
			break;
		case "delete":
			if (args.length > 1)
				po.recycle(sender.getName(), Arrays.copyOfRange(args, 1, args.length));
			else
				status = 501;
			break;
		case "reply":
			if (args.length == 1)
				status = po.reply(sender.getName(), "Re: ", null);
			else
				status = po.reply(sender.getName(), "Re: ", Arrays.copyOfRange(args, 1, args.length));
			break;
		case "replyall":
			if (args.length == 1)
				status = po.replyAll(sender.getName(), "Re: ", null);
			else
				status = po.replyAll(sender.getName(), "Re: ", Arrays.copyOfRange(args, 1, args.length));
			break;
		case "forward":
			if (args.length < 2)
				status = 501;
			else if (args.length == 2)
				status = po.forward(sender.getName(), args[1], "Fw: ", null);
			else
				status = po.forward(sender.getName(), args[1], "Fw: ", Arrays.copyOfRange(args, 2, args.length));
			break;
		case "help":
			String h = "§EAvailable mail (m) subcommands (aliases in parenthesis)`";
			h += "() inbox = get folder content (default with no subcommand)`";
			h += "sent = get folder content`";
			h += "trash = get folder content`";
			h += "NUMBER = type mail id to read it`";
			h += "(d) delete NUMBER1 NUMBER2 ... = delete mail(s)`";
			h += "(s) send TO1,TO2 SUBJECT \\ MESSAGE = send mail (\\=newline)`";
			h += "(r) reply [ADDITIONAL MESSAGE] = reply last viewed`";
			h += "(ra) replyall [ADDITIONAL MESSAGE] = replyall last viewed`";
			h += "(f) forward TO1,TO2 [ADDITIONAL MESSAGE] = ditto";
			for (String l: h.split("`"))
				sender.sendMessage(l);
			break;
		default:
			// retrieve: must supply a mail number
			int id;
			try
			{
				id = Integer.parseInt(args[0]);
			}
			catch (NumberFormatException e)
			{
				status = 500;
				break;
			}
			
			// looks ok
			String m  = po.retrieve(sender.getName(), id);
			if (m == null)
				status = 530;
			else
				for (String l: m.split("\n"))
					sender.sendMessage(l);
			break;
		}
		
		if (status != 250) sender.sendMessage(errMsg(status));
		return true;
	}
	
	// UTILITY
	
	private String errMsg(int code)
	{
		switch (code)
		{
		case 250:
			// 250 Requested mail action okay, completed
			return "";
		case 500:
			return "§D500 Command unrecognised";
		case 501:
			return "§D501 Syntax error in parameters or arguments";
		case 530:
			return "§D530 Access denied";
		case 553:
			return "§D553 Mailbox name not allowed";
		case 550:
			return "§D550 Mailbox unavailable";
		}
		return "§D451 Local error in processing";
	}
}
