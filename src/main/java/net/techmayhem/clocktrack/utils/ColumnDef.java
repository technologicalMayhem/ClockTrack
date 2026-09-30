package net.techmayhem.clocktrack.utils;

import java.util.function.Function;

public record ColumnDef<T>(String name, Function<T, String> valueMap) {
}
