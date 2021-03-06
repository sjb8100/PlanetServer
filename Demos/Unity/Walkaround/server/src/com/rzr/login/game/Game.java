package com.rzr.login.game;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import planetserver.session.UserSession;
import planetserver.network.PsObject;

import com.rzr.extension.WorldExtension;
import com.rzr.util.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import util.UserHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mike
 */
public class Game
{
    private static final Logger logger = LoggerFactory.getLogger(Game.class);
    
    private ConcurrentHashMap<Integer, Player> _users;
    
    private WorldExtension _extension;
    
    public Game(WorldExtension extension)
    {
        _extension = extension;
        
        _users = new ConcurrentHashMap<Integer, Player>();
    }
    
    public Player userAdded(UserSession session)
    {
        if (_users.containsKey(session.getId()))
            return null;
  
        Player player = new Player(session);
        _users.put(session.getId(), player);
        
        return player;
    }
    
    public void userLeft(UserSession session)
    {
        _users.remove(session.getId());
      
        // user wouldn't be in a room if they haven't logged in yet
        if (session.getCurrentRoom().length() > 1)
        {
            List<UserSession> users = UserHelper.getRecipientsList(_extension.getRoomManager().getRoom(session.getCurrentRoom()), session);

            if (users.size() > 0)
            {
                PsObject psobj = new PsObject();
                psobj.setString(Field.PlayerName.getCode(), session.getUserInfo().getUserid());

                _extension.send(PlayerCommand.getCommand(PlayerCommand.PlayerEnum.LEAVE), psobj, users);
            }
        }
    }
    
    public void start(UserSession session, PsObject params)
    {
        int type = params.getInteger(Field.PlayerType.getCode());
        List<Integer> position = params.getIntegerArray(Field.PlayerPosition.getCode());
   
        // let all the other players know a player just joined
        List<UserSession> users = UserHelper.getRecipientsList(_extension.getRoomManager().getRoom(session.getCurrentRoom()), session);
        
        if (users.size() > 0)
        {
            PsObject psobj = new PsObject();
            psobj.setString(Field.PlayerName.getCode(), session.getUserInfo().getUserid());
            psobj.setInteger(Field.PlayerType.getCode(), type);
            psobj.setIntegerArray(Field.PlayerPosition.getCode(), position);

            _extension.send(PlayerCommand.getCommand(PlayerCommand.PlayerEnum.INFOGROUP), psobj, users);
        }

        // let the player that just joined know about all the users currently in the game
        if (_users.size() > 0)
        {        
            List<PsObject> players = new ArrayList<PsObject>();

            for (Map.Entry<Integer, Player> entry : _users.entrySet()) 
            { 
                Player player = entry.getValue();
                
                PsObject obj = new PsObject();
                obj.setString(Field.PlayerName.getCode(), player.getUserSession().getUserInfo().getUserid());
                obj.setInteger(Field.PlayerType.getCode(), player.getType()); 
                obj.setIntegerArray(Field.PlayerPosition.getCode(), Arrays.asList(player.getPositionX(), player.getPositionY()));
                players.add(obj);
            }
            
            PsObject psobj = new PsObject();
            psobj.setPsObjectArray(Field.PlayerObj.getCode(), players);
        
            _extension.send(PlayerCommand.getCommand(PlayerCommand.PlayerEnum.INFOPLAYER), psobj, session);
        }
        
        Player player = userAdded(session);
        player.setType(type);
        player.setPositionX(position.get(0));
        player.setPositionY(position.get(1));
    }
    
    public void move(UserSession session, PsObject params)
    {
        List<Integer> position = params.getIntegerArray(Field.PlayerPosition.getCode());
        
        Player player = _users.get(session.getId());
        player.setPositionX(position.get(0));
        player.setPositionY(position.get(1));
        
        List<UserSession> users = UserHelper.getRecipientsList(_extension.getRoomManager().getRoom(session.getCurrentRoom()), session);
        
        PsObject psobj = new PsObject();
        psobj.setString(Field.PlayerName.getCode(), session.getUserInfo().getUserid());
        psobj.setIntegerArray(Field.PlayerPosition.getCode(), Arrays.asList(position.get(0), position.get(1)));
        
        _extension.send(PlayerCommand.getCommand(PlayerCommand.PlayerEnum.MOVE), psobj, users);
    }
    
    public void shoot(UserSession session, PsObject params)
    {
        List<Integer> position = params.getIntegerArray(Field.PlayerPosition.getCode());
        List<Integer> heading = params.getIntegerArray(Field.PlayerHeading.getCode());
        
         List<UserSession> users = UserHelper.getRecipientsList(_extension.getRoomManager().getRoom(session.getCurrentRoom()), session);
        
        PsObject psobj = new PsObject();
        psobj.setString(Field.PlayerName.getCode(), session.getUserInfo().getUserid());
        psobj.setIntegerArray(Field.PlayerPosition.getCode(), Arrays.asList(position.get(0), position.get(1)));
        psobj.setIntegerArray(Field.PlayerHeading.getCode(), Arrays.asList(heading.get(0), heading.get(1)));
        
        _extension.send(PlayerCommand.getCommand(PlayerCommand.PlayerEnum.SHOOT), psobj, users);
    }
}
