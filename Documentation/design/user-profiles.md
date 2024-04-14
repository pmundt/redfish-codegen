# Document Scope

This tool aims to provide useful tools for developers producing
Redfish-enabled products and services. This document captures use cases and
design intent for the items provided by this repository.

# User Profiles

The profiles below describe the target users of this project.

# P1. System Administrator

This person works for an organization not unlike yours and mine. They manage
real, physical pieces of hardware used by their organization. They want
ergonomic clients that enable visibility deep into the underbelly of their
infrastructure. They value efficiency when it comes to diagnosing failures,
and they care about having confidence in the configurations that they create
for their monitoring technology.

They may not know that the products they use are powered by this project,
but they depend on the components they rely on interacting in a seamless and
natural fashion.

Today, this user might rely on a technology like SNMP to gain insight into
their infrastructure, and they might leverage a tool like [The Dude][1] to
provide them the mechanism to use SNMP. They might also rely on
nuts-and-bolts tools like the net-snmp command line utilities.

# P2. System Integrator

This person creates solutions for their clients (may be their own
organization) by integrating existing products in novel ways. They challenge
the intended use cases of their vendors and serve in a variety of different
environments, which makes their needs difficult to predict. They care about
standardized interfaces, plug-and-play solutions, and ecosystems that
provide composable and scalable technology.

They may or may not know that the products they use are powered by this
project, but it's likely that the composability of their building blocks is
a direct result of the flexibility (or lack thereof) demonstrated by the
tools that this project produces.

Today, this user might create solutions that rely on a technology like SNMP,
and they might utilize a solution like `snmpd`, the net-snmp daemon, which
supports pluggable sub-agents via the AgentX protocol. They may also rely on
third-party applications that speak SNMP, like the [SNMP exporter for
Prometheus][2].

# P3. BMC Engineer

This person might work for a competitive cloud hyper-scalar, or for one of
their vendors. They create solutions that enable their customers to manage
specialized compute hardware, and they must balance the constraints imposed
on them by any number of vendors with the requirements of their customers.
Software architecture is the primary tool they apply to develop their
solutions. Because the solution space for their applications are so
demanding, and the constraints are so great, they demand ultimate
flexibility from their tools and their dependencies.

This person is likely a direct user of the tools developed under this
project. They may communicate with the maintainers of this software to merge
bug fixes and new features that enable flexibility in their solutions. They
may produce solutions used by users that fit either of the previous
profiles. They likely also rely on open source solutions like OpenBMC to
bring their development timelines into reasonable measure. They have
specialized needs based on the application of their hardware.

# P4. Embedded Software Engineer

This person works in a deeply embedded space. Likely, they develop
specialized, proprietary firmware that runs inside of compute hardware. They
expose an API over a high-speed fabric interconnect like PCIe or Ethernet.
Software is not a priority for their organization, so they feel pressure to
deliver the best solution within the time available--which may not be much
time at all. They value solutions that they don't have to integrate
horizontally into their software supply chain, because any upstream
dependencies are likely to have gone extinct by the next time they update
their software, or they don't have time to manage software supply chain in
their development timelines.

Today, this person might translate the Redfish specification by hand, and
provide it over a specialized bus like MCTP.

[1]: https://mikrotik.com/thedude
[2]: https://github.com/prometheus/snmp_exporter
