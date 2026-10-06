package dev.hitboxstudio.config;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Locale;

/** Writes colors as "#AARRGGBB" so the config file is readable; reads hex strings or plain numbers. */
public final class HexColorAdapter extends TypeAdapter<Integer> {
	@Override
	public void write(JsonWriter out, Integer value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.value(toHex(value));
		}
	}

	@Override
	public Integer read(JsonReader in) throws IOException {
		JsonToken token = in.peek();
		if (token == JsonToken.NULL) {
			in.nextNull();
			return 0xFFFFFFFF;
		}
		if (token == JsonToken.NUMBER) {
			return (int) in.nextLong();
		}
		Integer parsed = parse(in.nextString());
		return parsed != null ? parsed : 0xFFFFFFFF;
	}

	public static String toHex(int argb) {
		return String.format(Locale.ROOT, "#%08X", argb);
	}

	public static String toHexRgb(int argb) {
		return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
	}

	/** Accepts RRGGBB or AARRGGBB, with or without '#'. Returns null when invalid. */
	public static Integer parse(String text) {
		if (text == null) return null;
		String s = text.trim();
		if (s.startsWith("#")) s = s.substring(1);
		if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
		try {
			if (s.length() == 6) {
				return 0xFF000000 | Integer.parseInt(s, 16);
			}
			if (s.length() == 8) {
				return (int) Long.parseLong(s, 16);
			}
		} catch (NumberFormatException ignored) {
		}
		return null;
	}
}
