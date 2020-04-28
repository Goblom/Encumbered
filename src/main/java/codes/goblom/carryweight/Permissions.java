/*
 * Copyright (C) 2020 Bryan Larson
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
package codes.goblom.carryweight;

/**
 *
 * @author Bryan Larson
 */
public class Permissions {
    private static final String BASE = "carryweight.";
    
    /*
     * Commands
     */
    public static final String CALCULATE = BASE + "calculate";
    public static final String SET_MATERIAL = BASE + "setmaterial";
    public static final String SET_CARRY = BASE + "setcarry";
    public static final String RESET_CONFIG = BASE + "resetconfig";
    
    /*
     * Actions
     */
    public static final String BYPASS = BASE + "bypass";
}
