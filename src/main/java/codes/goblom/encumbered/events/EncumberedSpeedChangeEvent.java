/*
 * Copyright (C) 2022 Bryan Larson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package codes.goblom.encumbered.events;

import codes.goblom.encumbered.EncumberedPlayer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * @author Bryan Larson
 */
@AllArgsConstructor
@RequiredArgsConstructor
public class EncumberedSpeedChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static enum SpeedType {
        /**
         * For the setFlySpeed
         */
        FLY,
        /**
         * for the setWalkSpeed
         */
        WALK,
        /**
         * Called when player is no longer over encumbered
         */
        RESET
    }
    
    @Getter
    private final EncumberedPlayer player;
    
//    @Getter
//    private final float fromSpeed;
    
    @Getter
    @Setter
    private float toSpeed;
    
    @Getter
    private final SpeedType speedType;
}
