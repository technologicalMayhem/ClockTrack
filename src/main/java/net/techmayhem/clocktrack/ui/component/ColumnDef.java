package net.techmayhem.clocktrack.ui.component;

import java.util.function.Function;

public record ColumnDef<T>(String name, Function<T, String> valueMap) {}
