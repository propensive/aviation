[<img alt="GitHub Workflow" src="https://img.shields.io/github/actions/workflow/status/propensive/aviation/main.yml?style=for-the-badge" height="24">](https://github.com/propensive/aviation/actions)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.gg/7b6mpF6Qcf)
<img src="/doc/images/github.png" valign="middle">

# Aviation

__Representations of time, in its various forms__

The representation of time is complex, in part because the domain is
intrinsically complicated, and in part because we mean mean by "time" is
different in different contexts. _Aviation_ is an attempt to rationalize this
complexity by providing immutable representations of a variety concepts
relating to time and operations between them, taking advantage of the
opportunities Scala offers to make these APIs as intuitive as possible.

## Features

- representations points in time and lengths of time, in both exact/universal and civil forms
- types are all immutable and typesafe
- intuitive constructors for civil date and time values
- customisable rules for adding civil time units
- supports different calendar systems
- use and convert between different timezones and calendar systems


## Availability

Aviation has not yet been published as a binary.

## Getting Started

_Aviation_ provides a variety of types representing and working with different time-related concepts.

### On Time

_Time_ is a complicated concept in several ways. On a physical abstraction, we
can pinpoint an instant in time and order it as happening _before_ or _after_
another instant, provided they happen in the same place. We can consider the
difference between two instants, and measure it by comparison to reference
events that occur with a known regularly. We can relate _time_ to _distance_
using the speed of light, to [partially
order](https://en.wikipedia.org/wiki/Special_relativity) events in different
locations.

In a geological and heliocentric context, we talk about instants and durations
with reference to the rotation of the Earth around the sun, the rotation of
Earth on its axis, and the phase of the moon, as well as with arbitrary
subdivisions based on historical convention and [highly composite
numbers](https://en.wikipedia.org/wiki/Highly_composite_number), and sometimes
religious convention.

The measurements we use may have different meanings in different locations on
Earth (or conversely, the same measurement may be represented differently in
different locations), and different systems for representing the measurements
have been in use at different times in different places. The relationships
between different units of measurement are [not usually
decimal](https://en.wikipedia.org/wiki/12-hour_clock), [not always
constant](https://en.wikipedia.org/wiki/Thirty_Days_Hath_September), not always
exact subdivisions, and [may not even be known in
advance](https://en.wikipedia.org/wiki/Leap_second). The same representation
may refer to distinct events in the same place, and some representations may
not represent valid events on a handful of occasions. The system of
representation may depend on the prevailing political authority at the time,
which can depend on war.

_Aviation_ makes a bold effort to rationalize, systematize and encode this
complexity.



## Status

Aviation is classified as __fledgling__. For reference, Scala One projects are
categorized into one of the following five stability levels:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

Projects at any stability level, even _embryonic_ projects, are still ready to
be used, but caution should be taken if there is a mismatch between the
project's stability level and the importance of your own project.

Aviation is designed to be _small_. Its entire source code currently consists
of 762 lines of code.

## Building

Aviation can be built on Linux or Mac OS with [Fury](/propensive/fury), however
the approach to building is currently in a state of flux, and is likely to
change.

## Contributing

Contributors to Aviation are welcome and encouraged. New contributors may like to look for issues marked
<a href="https://github.com/propensive/aviation/labels/beginner">beginner</a>.

We suggest that all contributors read the [Contributing Guide](/contributing.md) to make the process of
contributing to Aviation easier.

Please __do not__ contact project maintainers privately with questions unless
there is a good reason to keep them private. While it can be tempting to
repsond to such questions, private answers cannot be shared with a wider
audience, and it can result in duplication of effort.

## Author

Aviation was designed and developed by Jon Pretty, and commercial support and training is available from
[Propensive O&Uuml;](https://propensive.com/).



## Name

_Tempus fugit_, or, _time flies_.

In general, Scala One project names are always chosen with some rationale, however it is usually
frivolous. Each name is chosen for more for its _uniqueness_ and _intrigue_ than its concision or
catchiness, and there is no bias towards names with positive or "nice" meanings—since many of the
libraries perform some quite unpleasant tasks.

Names should be English words, though many are obscure or archaic, and it should be noted how
willingly English adopts foreign words. Names are generally of Greek or Latin origin, and have
often arrived in English via a romance language.

## License

Aviation is copyright &copy; 2023 Jon Pretty & Propensive O&Uuml;, and is made available under the
[Apache 2.0 License](/license.md).
