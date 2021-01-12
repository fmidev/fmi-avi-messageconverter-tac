# TAC Converter



## Design Principles
For maximum reusability and flexibility the TAC conversion process is built using interfaces and generics. The current Implementation is spring configurable and aspires to functional programming ideals of immutability and pure functions.

## Parser
Converting a TAC message to A POJO is a broadly a four step process.

1. <b>Tokenizing</b><br/>
The incoming message is tokenized into into a list of individual strings. Each string (including whitespace) receives an entry in the resulting list.

2. <b>Lexing</b><br/>
Combination rules for lexing are defined in <b>fi.fmi.avi.converter.tac.conf.Lexing</b>. <b>fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl</b> tokenizes and iterates through String list to check if combination rules can be applied to sequential tokens. If a rule matches the string is concatinated and stored in a <b>fi.fmi.avi.converter.tac.lexer.LexemeSequence</b> object.

3. <b>Identifying</b><br/>
Once the combination rules have been applied the list is iterated again to see if the lexemes can be identified. identification classes are found in the package <b>fi.fmi.avi.converter.tac.lexer.impl.token</b>. The identification process uses regular expressions to identify lexemes and can include rules that check which lexeme precedes the the one being identified. For this reason the list is looped multiple times and priority rules may be applied to make the identification process more efficient. When a token is identified the relevant data is extracted and placed in the data field of the Lexeme object and the object is given an Identity.

4. <b>Initializing POJO</b><br/>
The LexemeSequence is iterated and the data from each Lexeme object is placed in the appropriate place in the POJO. Once the data is extracted the object is initialized as an immutable object.

### Related Classes
| Class        | Description           |
| ------------- |:-------------:|
| <b>fi.fmi.avi.converter.tac.conf.Lexing</b>|This is where the lexingfactories and combination rules are defined. <br/>Lexing factories are defined as beans that return a fi.fmi.avi.converter.tac.lexer.LexingFactoryImpl object that has combination rules added to it. <br/>Combination rules are methods that return a List<Predicate<String>> object into which you add one or more regular Predicate<String> objects that for String comparisons.
| <b>fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl</b>| This class is responsible for tokenizing the incoming String message and the iterating through the list to apply combination rules and identities.|
| <b>fi.fmi.avi.converter.tac.lexer.LexemeSequence</b| A TAC encoded version of the whole message. Implementations should return the exactly same TAC message.|
| <b>fi.fmi.avi.converter.tac.lexer.impl.token</b>| This is a package that contains classes that are specific to each token that is extracted from a TAC message. These classes should implement LexemeVisitor or extend an abstract class that does. These classes are mainly responsible for matching Strings and extracting data contained in lexemes. If a match is found this is also where identities are assigned to lexemes.|
| <b>fi.fmi.avi.converter.tac.EXAMPLE_PACKAGE.SERIALIZER_CLASS</b>|Parsing code is placed in this class. The parser class should extend <b>fi.fmi.avi.converter.tac.AbstractTACParser</b>|

## Serializer
- When converting a Java object to TAC message the serializer class should extend <b>fi.fmi.avi.converter.tac.AbstractTACSerializer</b> and the generic value should be defined as the object type that is being serialized.
- The serialization code should be placed in the overridden method <b>tokenizeMessage(AviationWeatherMessageOrCollection, ConversionHints)</b>
- Serializer should utilize the Reconstructor subclasses that are placed in the token classes under the <b>fi.fmi.avi.converter.tac.lexer.impl.token</b> package.
- Reconstructor subclasses correspond to LexemIdentities. The relationship between LexemeIdentities and Reconstructors is defined in <b>fi.fmi.avi.converter.tac.conf.Seializing</b>
- New message tokenizers are created and set in <b>fi.fmi.avi.converter.tac.conf.Seializing</b> class


### Related Classes
| Class| Descripiton|
|---|:---:|
| <b>fi.fmi.avi.converter.tac.AbstractTACSerializer</b>|Serializer code should be placed here. The serializer class extends <b>fi.fmi.avi.converter.tac.AbstractTACSerializer</b>|
| <b>fi.fmi.avi.converter.tac.lexer.impl.token.EXAMPLE_CLASS.Reconstructor</b>|Reconstructor classes extend calsses that implement the <b>fi.fmi.avi.converter.tac.lexer.impl</b> interface. The reconstructors job is to construct a TAC string interpretation of the field that the main class identifies. The String and its identity is then added to a Lexeme and returned|
| <b>fi.fmi.avi.converter.tac.conf.Seializing</b>|Tokenizers and reconstructor relationships are defined in this class. When adding support for a new message type, the serializer class needs to be defined as a private variable and initialized in the tacTokenizer method. A condition for the new message type also needs to be added to the tokenizeMessage method.<br/> This class is also where the relationships between a LexemeIdentity and a Reconstructor subclass is defined.|
| <b>TEXT</b>|Text|

## Spring Configuration Instructions
1. create class that inherits AviMessageSpecificConverter<?, ?> and fills the generic values
2. In <b>fi.fmi.avi.converter.tac.conf.Parsing</b> define a bean that returns fi.fmi.avi.converter.AviMessageSpecificConverter
3. Initialize an immutable variable of type <b>fi.fmi.avi.converter.ConversionSpecification</b> for the conversion you wish to preform in <b>fi.fmi.avi.converter.tac.conf.TACConversion</b>
4. Import <b>fi.fmi.avi.converter.tac.conf.TACConverter</b> into your spring configuration file and autowire the AviMessageSpecificConverter bean you created in step 2.
Then create a <b>fi.fmi.avi.converter.AviMessageConverter</b> bean and set MessageSpecificConverter values.

## Adding Message Type Support
  ### Parser
  1. Create a new package under <b>fi.fmi.avi.converter.tac</b> for the message type that is being adding
  2. Add serializer class that extends *** and implement necessary methods
  3. Create LexingFactory and combination rules
  4. create bean and add to configuration
  5. Create LexemIdentities (Same ones are used for Parsing and serializing)
  6. Create Token classes that extend ****
  7. Add parsing code

 ### Serializer
 1. Create a new package under <b>fi.fmi.avi.converter.tac</b> for the message type that is being adding
 2. Add serializer class that extends <b>fi.fmi.avi.converter.tac.AbstractTACSerializer</b> and implement required methods
 3. create bean and add to configuration
 4. Create LexemeIdentities  with a uniquely identifying name and data types for the data contained in that TAC field (Same Identities are used for Parsing and serializing)
 5. Classes in fi.fmi.avi.converter.tac.lexer.impl.token extend a version of LexemeVisitor. These classes are used for parsing. Reconstructors are added as subclasses to these.
  - Reconstructors should extend an implemetation of fi.fmi.avi.converter.tac.lexer.impl.TACTokenReconstructor
 6. Serializer code should appendToken on each for each required element in the POJO and the resulting Lexeme should be added to a LexemeSequence which is the converted into the final serialized message and returned in a ConversionResult Object.
