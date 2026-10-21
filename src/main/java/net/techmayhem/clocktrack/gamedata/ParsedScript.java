package net.techmayhem.clocktrack.gamedata;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/// A script that was parsed successfully, with all of its entries resolved to roles.
///
/// @param name  The name from the script's `_meta` entry, or null if there is none
/// @param roles All roles of the script by id, in script order
public record ParsedScript(@Nullable String name, Map<String, Role> roles) {}
