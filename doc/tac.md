# TAC Converter


## Design Principles

For maximum reusability and flexibility the TAC conversion process is built using interfaces and generics. The current Implementation is spring configurable and aspires to functional programming ideals of immutability and pure functions.

## Parser
Converting a TAC message to A POJO is a broadly a four step process.

1. **Tokenizing**  
   The incoming message is tokenized into into a list of individual strings. Each string (including whitespace) receives an entry in the resulting list.

2. **Lexing**  
   `fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl` tokenizes and iterates through `String` list to check if combination rules can be applied to
   sequential tokens. If a rule matches the string is concatenated and stored in a `fi.fmi.avi.converter.tac.lexer.LexemeSequence` object. Combination rules for
   lexing are defined in `fi.fmi.avi.converter.tac.conf.Lexing`.

3. **Identifying**  
   Once the combination rules have been applied the list is iterated again to see if the lexemes can be identified. identification classes are found in the
   package **fi.fmi.avi.converter.tac.lexer.impl.token**. The identification process uses regular expressions to identify lexemes and can include rules that
   check which lexeme precedes the the one being identified. For this reason the list is looped multiple times and priority rules may be applied to make the
   identification process more efficient. When a token is identified the relevant data is extracted and placed in the data field of the Lexeme object and the
   object is given an Identity.

4. **Build POJO**  
   The LexemeSequence is iterated and the data from each identified Lexeme object is placed in the appropriate place in the POJO. Once the data is extracted the
   object is initialized as an immutable object.

### Related Classes

* `fi.fmi.avi.converter.tac.conf.Lexing`  
  This class is where the lexing factories and combination rules are defined.
  * Lexing factories are defined as beans that return a `fi.fmi.avi.converter.tac.lexer.LexingFactoryImpl` object that has combination rules added to it.
  * Combination rules are methods that return a `List<Predicate<String>>` object into which you add one or more regular `Predicate<String>` objects that for
    String comparisons.

* `fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl`  
  This class is responsible for tokenizing the incoming String message and the iterating through the list to apply combination rules and identities.

* `fi.fmi.avi.converter.tac.lexer.LexemeSequence`  
  A TAC encoded version of the whole message. Implementations should return the exactly same TAC message.

* `fi.fmi.avi.converter.tac.lexer.impl.token`  
  This is a package that contains classes that are specific to each token that is extracted from a TAC message. These classes should implement `LexemeVisitor`
  or extend an abstract class that does. These classes are mainly responsible for matching Strings and extracting data contained in lexemes. If a match is found
  this is also where identities are assigned to lexemes.

* `fi.fmi.avi.converter.tac.EXAMPLE_PACKAGE.SERIALIZER_CLASS`  
  Parsing code is placed in this class. The parser class should extend `fi.fmi.avi.converter.tac.AbstractTACParser`

## Serializer

In order to serialize an object the converter structure requires that a LexemeSequence is created out of the POJO and that LexemeSequence is then converted into
a String.

* When converting a Java object to TAC message the serializer class should extend `fi.fmi.avi.converter.tac.AbstractTACSerializer` and the generic value should
  be defined as the object type that is being serialized.
* The serialization code should be placed in the overridden method `tokenizeMessage(AviationWeatherMessageOrCollection, ConversionHints)`
* Serializer should utilize the Reconstructor subclasses that are found in the token classes under the `fi.fmi.avi.converter.tac.lexer.impl.token` package.
* Reconstructor subclasses correspond to LexemIdentities. The relationship between LexemeIdentities and Reconstructors is defined
  in `fi.fmi.avi.converter.tac.conf.Serializing`.
* New message tokenizers are created and set in `fi.fmi.avi.converter.tac.conf.Serializing` class.

### Related Classes

* `fi.fmi.avi.converter.tac.AbstractTACSerializer`  
  Serializer code should be placed here. The serializer class extends `fi.fmi.avi.converter.tac.AbstractTACSerializer`

* `fi.fmi.avi.converter.tac.lexer.impl.token.EXAMPLE_CLASS.Reconstructor`  
  Reconstructor classes extend calsses that implement the `fi.fmi.avi.converter.tac.lexer.impl` interface. The reconstructors job is to construct a TAC string
  interpretation of the field that the main class identifies. The String and its identity is then added to a Lexeme and returned

* `fi.fmi.avi.converter.tac.conf.Serializing`  
  Tokenizers and reconstructor relationships are defined in this class. When adding support for a new message type, the serializer class needs to be defined as
  a private variable and initialized in the tacTokenizer method. A condition for the new message type also needs to be added to the tokenizeMessage method.  
  This class is also where the relationships between a LexemeIdentity and a Reconstructor subclass is defined.

## Spring Configuration Instructions

1. Create class that inherits `AviMessageSpecificConverter<?, ?>` and fills the generic values.
2. In `fi.fmi.avi.converter.tac.conf.Parsing` define a bean that returns `fi.fmi.avi.converter.AviMessageSpecificConverter`.
3. Initialize an immutable variable of type `fi.fmi.avi.converter.ConversionSpecification` for the conversion you wish to preform
   in `fi.fmi.avi.converter.tac.conf.TACConversion`.
4. Import `fi.fmi.avi.converter.tac.conf.TACConverter` into your spring configuration file and autowire the AviMessageSpecificConverter bean you created in step
   2. Then create a `fi.fmi.avi.converter.AviMessageConverter` bean and set MessageSpecificConverter values.

## Adding a New Message Type Support

### Parser

1. Create a new package under `fi.fmi.avi.converter.tac` for the message type that is being added
2. Add serializer class. The serializer class should extend `fi.fmi.avi.converter.tac.AbstractTACParser`
3. Create combination rules in  `fi.fmi.avi.converter.tac.conf.Lexing` and add the rules to the LexingFactory bean found in the same class.
4. Add a ConversionSpecification in `fi.fmi.avi.converter.tac.conf.TACConverter`
4. Create LexemeIdentities with a uniquely identifying name and data types for the data contained in that TAC field. Lexeme identities are found
   in `fi.fmi.avi.converter.tac.lexer.LexemeIdentity` (Same Identities are used for Parsing and serializing)
6. Create Token classes. Token classes should extend an implementation of the `fi.fmi.avi.converter.tac.lexer.LexemeVisitor` interface.
7. Add parsing code
8. Add Your conversion specification to your AviMessageConverter bean

### Serializer

1. Create a new package under `fi.fmi.avi.converter.tac` for the message type that is being adding
2. Add serializer class that extends `fi.fmi.avi.converter.tac.AbstractTACSerializer` and implement required methods
3. Add a ConversionSpecification in `fi.fmi.avi.converter.tac.conf.TACConverter`
4. Create LexemeIdentities with a uniquely identifying name and data types for the data contained in that TAC field. Lexeme identities are found
   in `fi.fmi.avi.converter.tac.lexer.LexemeIdentity` (Same Identities are used for Parsing and serializing)
5. Classes in `fi.fmi.avi.converter.tac.lexer.impl.token` extend a version of LexemeVisitor implementation. These classes are used for parsing. Reconstructors
   are added as subclasses to these.

* Reconstructors should extend an implementation of `fi.fmi.avi.converter.tac.lexer.impl.TACTokenReconstructor`.

6. Add serializer code that builds a LexemeSequence
