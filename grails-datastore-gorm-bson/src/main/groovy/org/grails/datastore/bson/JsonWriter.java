package org.grails.datastore.bson;

import org.bson.*;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.springframework.util.Base64Utils;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A more limited {@link org.bson.json.JsonWriter} implementation that ignores behaviour specific to MongoDB and produces more compat output
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class JsonWriter extends AbstractBsonWriter {

    static final char OPEN_BRACKET = '[';
    static final char CLOSE_BRACKET = ']';
    static final char OPEN_BRACE = '{';
    static final char CLOSE_BRACE = '}';
    static final char COLON = ':';
    static final char COMMA = ',';
    static final char SPACE = ' ';
    static final char NEW_LINE = '\n';
    static final char QUOTE = '"';
    public static final String BOOLEAN_TRUE = "true";
    public static final String BOOLEAN_FALSE = "false";
    public static final String NULL = "null";
    public static final String ISO_8601 = "yyyy-MM-dd'T'HH:mmZ";
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final char FORWARD_SLASH = '/';

    protected final Writer writer;
    protected final JsonWriterSettings settings;

    public JsonWriter(Writer target) {
        this(target, new JsonWriterSettings());
    }

    public JsonWriter(Writer target, JsonWriterSettings settings) {
        super(settings);
        this.writer = target;
        this.settings = settings;
        setContext(new Context(null, BsonContextType.TOP_LEVEL, ""));
    }

    @Override
    protected void doWriteStartDocument() {
        try {
            if (getState() == State.VALUE || getState() == State.SCOPE_DOCUMENT) {
                writeNameHelper(getName());
            }
            BsonContextType contextType = (getState() == State.SCOPE_DOCUMENT) ? BsonContextType.SCOPE_DOCUMENT : BsonContextType.DOCUMENT;
            setContext(new Context(getContext(), contextType, settings.getIndentCharacters()));

            writer.write(OPEN_BRACE);
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    @Override
    protected void doWriteEndDocument() {
        try {
            writer.write(CLOSE_BRACE);
            if (getContext().getContextType() == BsonContextType.SCOPE_DOCUMENT) {
                setContext(getContext().getParentContext());
                writeEndDocument();
            } else {
                setContext(getContext().getParentContext());
            }
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    @Override
    protected void doWriteStartArray() {
        try {
            writeNameHelper(getName());
            writer.write(OPEN_BRACKET);
            setContext(new Context(getContext(), BsonContextType.ARRAY, settings.getIndentCharacters()));
        } catch (IOException e) {
            throwBsonException(e);
        }

    }
    @Override
    protected void doWriteEndArray() {
        try {
            writer.write(CLOSE_BRACKET);
        } catch (IOException e) {
            throwBsonException(e);
        }
        setContext(getContext().getParentContext());

    }

    @Override
    protected void doWriteBinaryData(BsonBinary value) {
        try {
            writeNameHelper(getName());
            byte[] data = value.getData();
            writer.write(Base64Utils.encodeToString(data));
            setState(getNextState());
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    @Override
    protected void doWriteBoolean(boolean value) {
        try {
            writeNameHelper(getName());
            writer.write(value ? BOOLEAN_TRUE : BOOLEAN_FALSE);
            setState(getNextState());
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    @Override
    protected void doWriteDateTime(long value) {
        DateFormat df = new SimpleDateFormat(ISO_8601);
        df.setTimeZone(UTC);
        try {
            writeNameHelper(getName());
            writer.write(QUOTE);
            Date date = new Date(value);
            writer.write( df.format(date) );
            writer.write(QUOTE);
            setState(getNextState());
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    @Override
    protected void doWriteDBPointer(BsonDbPointer value) {
        // no-op
    }

    @Override
    protected void doWriteDouble(double value) {
        try {
            writeNameHelper(getName());
            writer.write(Double.toString(value));
            setState(getNextState());
        } catch (IOException e) {
            throwBsonException(e);
        }

    }

    @Override
    protected void doWriteInt32(int value) {
        try {
            writeNameHelper(getName());
            writer.write(Integer.toString(value));
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    @Override
    protected void doWriteInt64(long value) {
        try {
            writeNameHelper(getName());
            if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                writer.write(Long.toString(value));
            }
            else {
                writer.write(QUOTE);
                writer.write(Long.toString(value));
                writer.write(QUOTE);
            }
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    @Override
    protected void doWriteJavaScript(String value) {
        // no-op
    }

    @Override
    protected void doWriteJavaScriptWithScope(String value) {
        // no-op
    }

    @Override
    protected void doWriteMaxKey() {
        // no-op
    }

    @Override
    protected void doWriteMinKey() {
        // no-op
    }

    @Override
    protected void doWriteNull() {
        try {
            writeNameHelper(getName());
            writer.write(NULL);
        } catch (IOException e) {
            throwBsonException(e);
        }

    }

    @Override
    protected void doWriteObjectId(ObjectId value) {
        try {
            writeNameHelper(getName());
            writer.write(value.toString());
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    @Override
    protected void doWriteRegularExpression(BsonRegularExpression regularExpression) {
        try {
            switch (settings.getOutputMode()) {
                case STRICT:
                    writeStartDocument();
                    writeString("$regex", regularExpression.getPattern());
                    writeString("$options", regularExpression.getOptions());
                    writeEndDocument();
                    break;
                default:
                    writeNameHelper(getName());
                    writer.write(FORWARD_SLASH);
                    String escaped = (regularExpression.getPattern().equals("")) ? "(?:)" : regularExpression.getPattern()
                            .replace("/", "\\/");
                    writer.write(escaped);
                    writer.write(FORWARD_SLASH);
                    writer.write(regularExpression.getOptions());
                break;
            }
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    @Override
    protected void doWriteString(final String value) {
        try {
            writeNameHelper(getName());
            writeStringHelper(value);
        } catch (IOException e) {
            throwBsonException(e);
        }

    }

    @Override
    protected void doWriteSymbol(String value) {
        // no-op
    }

    @Override
    protected void doWriteTimestamp(BsonTimestamp value) {
        // no-op
    }

    @Override
    protected void doWriteUndefined() {
        try {
            writeNameHelper(getName());
            writer.write("undefined");
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            throwBsonException(e);
        }
    }

    protected void writeNameHelper(final String name) throws IOException {
        switch (getContext().getContextType()) {
            case ARRAY:
                // don't write Array element names in JSON
                if (getContext().hasElements) {
                    writer.write(COMMA);
                }
                break;
            case DOCUMENT:
            case SCOPE_DOCUMENT:
                if (getContext().hasElements) {
                    writer.write(COMMA);
                }
                if (settings.isIndent()) {
                    writer.write(settings.getNewLineCharacters());
                    writer.write(getContext().indentation);
                }
                writeStringHelper(name);
                writer.write(COLON);
                break;
            case TOP_LEVEL:
                break;
            default:
                throw new BSONException("Invalid contextType.");
        }

        getContext().hasElements = true;
    }

    private void throwBsonException(IOException e) {
        throw new BSONException("Cannot write to writer writer: " + e.getMessage(), e);
    }

    @Override
    protected Context getContext() {
        return (Context) super.getContext();
    }

    private void writeStringHelper(final String str) throws IOException {
        writer.write('"');
        for (final char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    writer.write("\\\"");
                    break;
                case '\\':
                    writer.write("\\\\");
                    break;
                case '\b':
                    writer.write("\\b");
                    break;
                case '\f':
                    writer.write("\\f");
                    break;
                case '\n':
                    writer.write("\\n");
                    break;
                case '\r':
                    writer.write("\\r");
                    break;
                case '\t':
                    writer.write("\\t");
                    break;
                default:
                    switch (Character.getType(c)) {
                        case Character.UPPERCASE_LETTER:
                        case Character.LOWERCASE_LETTER:
                        case Character.TITLECASE_LETTER:
                        case Character.OTHER_LETTER:
                        case Character.DECIMAL_DIGIT_NUMBER:
                        case Character.LETTER_NUMBER:
                        case Character.OTHER_NUMBER:
                        case Character.SPACE_SEPARATOR:
                        case Character.CONNECTOR_PUNCTUATION:
                        case Character.DASH_PUNCTUATION:
                        case Character.START_PUNCTUATION:
                        case Character.END_PUNCTUATION:
                        case Character.INITIAL_QUOTE_PUNCTUATION:
                        case Character.FINAL_QUOTE_PUNCTUATION:
                        case Character.OTHER_PUNCTUATION:
                        case Character.MATH_SYMBOL:
                        case Character.CURRENCY_SYMBOL:
                        case Character.MODIFIER_SYMBOL:
                        case Character.OTHER_SYMBOL:
                            writer.write(c);
                            break;
                        default:
                            writer.write("\\u");
                            writer.write(Integer.toHexString((c & 0xf000) >> 12));
                            writer.write(Integer.toHexString((c & 0x0f00) >> 8));
                            writer.write(Integer.toHexString((c & 0x00f0) >> 4));
                            writer.write(Integer.toHexString(c & 0x000f));
                            break;
                    }
                    break;
            }
        }
        writer.write('"');
    }


    /**
     * The context for the writer, inheriting all the values from {@link org.bson.AbstractBsonWriter.Context}, and additionally providing
     * settings for the indentation level and whether there are any child elements at this level.
     */

    public class Context extends AbstractBsonWriter.Context {
        private final String indentation;
        private boolean hasElements;

        /**
         * Creates a new context.
         *
         * @param parentContext the parent context that can be used for going back up to the parent level
         * @param contextType   the type of this context
         * @param indentChars   the String to use for indentation at this level.
         */
        public Context(final Context parentContext, final BsonContextType contextType, final String indentChars) {
            super(parentContext, contextType);
            this.indentation = (parentContext == null) ? indentChars : parentContext.indentation + indentChars;
        }

        @Override
        public Context getParentContext() {
            return (Context) super.getParentContext();
        }
    }
}
