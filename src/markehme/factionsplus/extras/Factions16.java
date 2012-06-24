package markehme.factionsplus.extras;

import java.lang.reflect.*;
import java.util.*;

import org.bukkit.*;

import markehme.factionsplus.*;

import com.google.common.collect.*;
import com.massivecraft.factions.*;
import com.massivecraft.factions.cmd.*;



public class Factions16 extends FactionsBase implements FactionsAny {
	
	private Method							mSetPeaceful		= null;
	private Method							methodUpdateHelp	= null;
	private Object							instanceOfCmdHelp	= null;
	private Field							fieldCmdHelp		= null;
	private Object							instField			= null;
	private Field							fHelpPages			= null;
	private ArrayList<ArrayList<String>>	instanceOfHelpPages	= null;
	private Method 							mSetChatMode		= null;
	private Method 							mGetChatMode		= null;
	
	//maps Factions 1.6 com.massivecraft.factions.struct.ChatMode  to FactionsAny.ChatMode
	private TwoWayHashMapOfNonNulls<Object, FactionsAny.ChatMode>	mapChatMode		= new TwoWayHashMapOfNonNulls<>();
		
	
	protected Factions16( ) {
		super();
		
		boolean failed = false;
		
		try {
			mSetPeaceful = Faction.class.getMethod( "setPeaceful", boolean.class );
			
			Class clas = Class.forName( "com.massivecraft.factions.cmd.CmdHelp" );
			
			methodUpdateHelp = clas.getMethod( "updateHelp" );
			
			Class fcmdroot = Class.forName( "com.massivecraft.factions.cmd.FCmdRoot" );
			
			fieldCmdHelp = fcmdroot.getField( "cmdHelp" );
			
			fHelpPages = clas.getField( "helpPages" );
			
			Class classChatMode=Class.forName("com.massivecraft.factions.struct.ChatMode");
			Reflective.mapEnums( mapChatMode, classChatMode, FactionsAny.ChatMode.class);
			
			Class classFPlayer= Class.forName( "com.massivecraft.factions.FPlayer" );
			mSetChatMode=classFPlayer.getMethod("setChatMode", classChatMode);
			mGetChatMode=classFPlayer.getMethod("getChatMode");
			
		} catch ( NoSuchMethodException e ) {// multi catch could've worked but unsure if using jdk7 to compile
			e.printStackTrace();
			failed = true;
		} catch ( SecurityException e ) {
			e.printStackTrace();
			failed = true;
		} catch ( ClassNotFoundException e ) {
			e.printStackTrace();
			failed = true;
		} catch ( NoSuchFieldException e ) {
			e.printStackTrace();
			failed = true;
		} finally {
			if ( failed ) {
				throw FactionsPlus.bailOut( "failed to hook into Factions 1.6.x" );
			}
		}
	}
	
	
	@Override
	public void setFlag( Faction forFaction, FactionsAny.FFlag whichFlag, Boolean whatState ) {
		boolean failed = false;
		try {
			switch ( whichFlag ) {
			case PEACEFUL:
				mSetPeaceful.invoke( forFaction, whatState );
				break;
			// add new flags here
			default:
				throw FactionsPlus.bailOut( "plugin author forgot to define a case to handle this flag: "
					+ whichFlag );
				// or forgot to put a "break;"
			}
			
		} catch ( IllegalAccessException e ) {
			e.printStackTrace();
			failed = true;
		} catch ( IllegalArgumentException e ) {
			e.printStackTrace();
			failed = true;
		} catch ( InvocationTargetException e ) {
			e.printStackTrace();
			failed = true;
		} finally {
			if ( failed ) {
				throw FactionsPlus.bailOut( "failed to invoke " + mSetPeaceful );
			}
		}
	}
	
	
	private final static byte	howManyPerPage	= 5;
	private static byte			currentPerPage			= 0;
	private ArrayList<String>	pageLines		= null;
	
	
	@Override
	public void addSubCommand( FCommand subCommand ) {
		super.addSubCommand( subCommand );
		// for 1.6 need to add the command to help manually
		if ( null == instanceOfCmdHelp ) {
			boolean failed = false;
			try {
				// lazy init this(one time since plugin.onEnable()), cause on .init() was probably too soon
				instanceOfCmdHelp = fieldCmdHelp.get( P.p.cmdBase );
				methodUpdateHelp.invoke( instanceOfCmdHelp );// P.p.cmdBase.cmdHelp.updateHelp();
				instanceOfHelpPages = (ArrayList<ArrayList<String>>)fHelpPages.get( instanceOfCmdHelp );
			} catch ( IllegalAccessException e ) {
				e.printStackTrace();
				failed = true;
			} catch ( IllegalArgumentException e ) {
				e.printStackTrace();
				failed = true;
			} catch ( InvocationTargetException e ) {
				e.printStackTrace();
				failed = true;
			} finally {
				if ( failed ) {
					throw FactionsPlus.bailOut( "failed to invoke " + methodUpdateHelp );
				}
			}
		}
		
		
		if ( null == pageLines ) {
			pageLines = new ArrayList<String>();
			currentPerPage = 0;
		}
		
		pageLines.add( subCommand.getUseageTemplate( true ) );
		if ( currentPerPage >= howManyPerPage ) {
			instanceOfHelpPages.add( pageLines );
			pageLines = null;
		} else {
			currentPerPage++;
		}
		
	}
	
	
	@Override
	public final void finalizeHelp() {
		if ( null == instanceOfHelpPages ) {
			throw FactionsPlus.bailOut( "this should not happen, bad call order" );
		} else {
			instanceOfHelpPages.add( pageLines );
			pageLines = null;
			currentPerPage = 0;
		}
	}


	@Override
	public boolean setChatMode( ChatMode chatMode ) {
		boolean failed=false;
		try {
			mapChatMode.get( key )
			mSetChatMode.invoke(chatMode);
		} catch ( IllegalAccessException e ) {
			e.printStackTrace();
			failed=true;
		} catch ( IllegalArgumentException e ) {
			e.printStackTrace();
			failed=true;
		} catch ( InvocationTargetException e ) {
			e.printStackTrace();
			failed=true;
		}finally {
			if ( failed ) {
				throw FactionsPlus.bailOut( "failed to invoke " + mSetChatMode );
			}
		}
		return true;//even if there was actually no chatMode change when compared to the previous, true means it 
	}
}
