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
maximum line width. This is what Eclipse does, by default. Other IDEs should
be configured for same order to avoid trashing Git commit diffs.

## Classes
Classes are named in CapitalCamelCase. Avoid multiple successive capital
letters (for example, if we had IP address class, it would be IpAddress,
not IPAddress).

## Comments
Comments are usually added before the thing they are describing. For
single-line comments, putting them in the same line is also allowed.

Javadoc should be readable as plain text, even when HTML is used. Using
@tags is encouraged (@links can be very useful).

For multi-line comments, maximum line width is 80 characters. This is to ensure
that they are easily readable in text editor view where line width cannot
be automatically adjusted (most IDEs).

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
VarHandles can be used (and are usually preferred, since volatile provides
a lot of potentially *unnecessary* guarantees).

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
question is large. Atomic operations are always preferred over synchronization,
because they have much better performance in multi-core systems. Non-blocking
algorithms are good to have, but often very hard or impossible to implement;
thus, limiting code in Terra to them is not required.

Method parameters are usually effectively final. However, adding final modifier
is not done to avoid making method declarations overly long.

Methods that override others should always be annotated with @Override. They
follow guidelines set by Javadoc of method they are overriding (if found).
This allows quickly identifying implemented methods from methods that are
unique to implementation. For example, OffheapWorld has a lot of *public*
methods which would not be available in other world implementations.

## Blocks
Always use curly brackets to start and end blocks of code. Do:

```
if (condition) {
	doStuff();
}
```

While Java does allow leaving brackets out, it has potential of causing
"fun" bugs. For example, following statement will not be executed as it
should be:

```
if (condition)
	#doStuff();
cleanup();
```

Comments are not statements, so cleanup() will *not* be always executed.
You can probably imagine what problems that might cause.

## Anonymous Classes and Lambdas
Lambdas and anonymous classes are *quite* close to each other when it comes to
their implementation in OpenJDK. Neither is free to use, so in hotspots,
avoiding them might be beneficial.

In addition to that, using lambdas to make code modern, but also unreadable...
Just don't do it. Sometimes using them makes sense, other times, old is better.

## Thread Safety
Most of code in Terra has to be thread safe in some fashion. Sometimes, is is
done in locks (from standard library, or simple spin-loops). However, using
atomic operations such as compare-and-swap to avoid blocking is desired.

First, the less we block, the better use multiple CPU cores can be put to.
Second, nonblocking code makes Terra more resilient against operating system
scheduler quirks; if a scheduler stalls a thread that is holding an exclusive
lock, no progress will be made.

Concurrency is hard to get right. It should be possible for us to prove that
our code is safe (according to Java Memory Model). While we are currently only
targeting AMD64 CPUs, it would be a shame to write a ton of not portable code
in *Java*. Cases where we rely on x86's relatively strong memory ordering
should be avoided or at least isolated.

Note that currently, some code does neither. Sadly, parts of Terra were written
when I (bensku) did not have a good understanding about memory visibility.

## Pointers
Java lacks a dedicated type for pointers (until Project Panama lands), so longs
are used when pointing to offheap memory without a wrapper is needed. Still,
some arithmetic operations (such as division) rarely make sense on pointers,
so distinguishing them from just numbers is helpful.

For this purpose, Terra has an annotation
com.ritualsoftheold.terra.offheap.Pointer. All pointer-holding fields, method
parameters, return values and local variables should be annotated with it.
In future, annotation processing might be used to run basic sanity checks
against Terra, which missing @Pointer annotations would seriously undermine.