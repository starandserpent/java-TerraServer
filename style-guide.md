# Terra Code Style Guide
These guidelines apply to all Java source code in Terra.

Work in progress!

## File Format
All Java source files should use UTF-8 encoding. Currently, both LF and CRLF
line endings are acceptable, though LF is preferred.

Maximum line width is 120 characters, unless otherwise mentioned.

## Packages
Terra resides under package "com.ritualsoftheold.terra". Each of subprojects
has their own root package. Do not place Java code outside of these packages.

## Imports
Generally, import only a single class per line. Static imports are allowed
when it makes sense. In unit tests, importing all jUnit asserts in a single
line is allowed (but not mandatory).

Imports should be placed in groups, which are separated by empty lines.
First group is for standard library imports (e.g. java.nio.Path). After
that, each each TLD (com or net, for example), gets their own import group.
They are ordered alphabetically.

Imports inside groups are ordered alphabetically. Import statements have no
maximum line width.

## Classes
Classes are named in CapitalCamelCase. Avoid multiple successive capital
letters (for example, if we had IP address class, it would be IpAddress,
not IPAddress).

## Comments
Comments are usually added before the thing they are describing. For
single-line comments, putting them in the same line is also allowed.

Javadoc should be readable as plain text, even when HTML is used. Using
@tags is encouraged (@links can be very useful).

For multi-line comments, maximum line width is 80 characters.

## Fields
Fields are usually named in camelCase. Public static final fields
("constants") should be instead named in CAPITAL\_SNAKE\_CASE.

Declarations of them should have their parts in the following order:

1. Access modifier (public, protected or private)
2. Static (static)
3. Memory guarantee (final, volatile)
4. Type (boolean, byte, short, char, int, String, Object, ...)
5. Field name

When a field is set only in constructor, it should be final. This is especially
important to ensure memory visibility on all possible JVM implementations.

A field that is set from multiple threads can be set volatile. Alternatively,
VarHandles can be used (and are usually preferred).

Unless the class where field is is solely for data storage, or is private,
fields should rarely be made public. This is especially true if they are not
final.

## Methods
Methods are named in camelCase. Declarations of them should have their parts
in the following order:
1. Access modifier (public, protected or private)
2. Static (static)
3. Method-level synchronized (synchronization)
4. Return type
5. Method name and parameters

Method-level synchronization should be avoided, especially if the method in
question is large. Atomic operations are always preferred over synchronization.

Method parameters are usually effectively final. However, adding final modifier
is not done to avoid making method declarations overly long.

Methods that override others should always be annotated with @Override. They
follow guidelines set by Javadoc of method they are overriding (if found).

## Blocks
Always use curly brackets to start and end blocks.

## Pointers
When using longs as pointers, annotate them with @Pointer whenever possible.