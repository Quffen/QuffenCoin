# Libraries

| Name                     | Description |
|--------------------------|-------------|
| *libquffen_cli*         | RPC client functionality used by *quffen-cli* executable |
| *libquffen_common*      | Home for common functionality shared by different executables and libraries. Similar to *libquffen_util*, but higher-level (see [Dependencies](#dependencies)). |
| *libquffen_consensus*   | Stable, backwards-compatible consensus functionality used by *libquffen_node* and *libquffen_wallet* and also exposed as a [shared library](../shared-libraries.md). |
| *libquffenconsensus*    | Shared library build of static *libquffen_consensus* library |
| *libquffen_kernel*      | Consensus engine and support library used for validation by *libquffen_node* and also exposed as a [shared library](../shared-libraries.md). |
| *libquffenqt*           | GUI functionality used by *quffen-qt* and *quffen-gui* executables |
| *libquffen_ipc*         | IPC functionality used by *quffen-node*, *quffen-wallet*, *quffen-gui* executables to communicate when [`--enable-multiprocess`](multiprocess.md) is used. |
| *libquffen_node*        | P2P and RPC server functionality used by *quffend* and *quffen-qt* executables. |
| *libquffen_util*        | Home for common functionality shared by different executables and libraries. Similar to *libquffen_common*, but lower-level (see [Dependencies](#dependencies)). |
| *libquffen_wallet*      | Wallet functionality used by *quffend* and *quffen-wallet* executables. |
| *libquffen_wallet_tool* | Lower-level wallet functionality used by *quffen-wallet* executable. |
| *libquffen_zmq*         | [ZeroMQ](../zmq.md) functionality used by *quffend* and *quffen-qt* executables. |

## Conventions

- Most libraries are internal libraries and have APIs which are completely unstable! There are few or no restrictions on backwards compatibility or rules about external dependencies. Exceptions are *libquffen_consensus* and *libquffen_kernel* which have external interfaces documented at [../shared-libraries.md](../shared-libraries.md).

- Generally each library should have a corresponding source directory and namespace. Source code organization is a work in progress, so it is true that some namespaces are applied inconsistently, and if you look at [`libquffen_*_SOURCES`](../../src/Makefile.am) lists you can see that many libraries pull in files from outside their source directory. But when working with libraries, it is good to follow a consistent pattern like:

  - *libquffen_node* code lives in `src/node/` in the `node::` namespace
  - *libquffen_wallet* code lives in `src/wallet/` in the `wallet::` namespace
  - *libquffen_ipc* code lives in `src/ipc/` in the `ipc::` namespace
  - *libquffen_util* code lives in `src/util/` in the `util::` namespace
  - *libquffen_consensus* code lives in `src/consensus/` in the `Consensus::` namespace

## Dependencies

- Libraries should minimize what other libraries they depend on, and only reference symbols following the arrows shown in the dependency graph below:

<table><tr><td>

```mermaid

%%{ init : { "flowchart" : { "curve" : "basis" }}}%%

graph TD;

quffen-cli[quffen-cli]-->libquffen_cli;

quffend[quffend]-->libquffen_node;
quffend[quffend]-->libquffen_wallet;

quffen-qt[quffen-qt]-->libquffen_node;
quffen-qt[quffen-qt]-->libquffenqt;
quffen-qt[quffen-qt]-->libquffen_wallet;

quffen-wallet[quffen-wallet]-->libquffen_wallet;
quffen-wallet[quffen-wallet]-->libquffen_wallet_tool;

libquffen_cli-->libquffen_util;
libquffen_cli-->libquffen_common;

libquffen_common-->libquffen_consensus;
libquffen_common-->libquffen_util;

libquffen_kernel-->libquffen_consensus;
libquffen_kernel-->libquffen_util;

libquffen_node-->libquffen_consensus;
libquffen_node-->libquffen_kernel;
libquffen_node-->libquffen_common;
libquffen_node-->libquffen_util;

libquffenqt-->libquffen_common;
libquffenqt-->libquffen_util;

libquffen_wallet-->libquffen_common;
libquffen_wallet-->libquffen_util;

libquffen_wallet_tool-->libquffen_wallet;
libquffen_wallet_tool-->libquffen_util;

classDef bold stroke-width:2px, font-weight:bold, font-size: smaller;
class quffen-qt,quffend,quffen-cli,quffen-wallet bold
```
</td></tr><tr><td>

**Dependency graph**. Arrows show linker symbol dependencies. *Consensus* lib depends on nothing. *Util* lib is depended on by everything. *Kernel* lib depends only on consensus and util.

</td></tr></table>

- The graph shows what _linker symbols_ (functions and variables) from each library other libraries can call and reference directly, but it is not a call graph. For example, there is no arrow connecting *libquffen_wallet* and *libquffen_node* libraries, because these libraries are intended to be modular and not depend on each other's internal implementation details. But wallet code is still able to call node code indirectly through the `interfaces::Chain` abstract class in [`interfaces/chain.h`](../../src/interfaces/chain.h) and node code calls wallet code through the `interfaces::ChainClient` and `interfaces::Chain::Notifications` abstract classes in the same file. In general, defining abstract classes in [`src/interfaces/`](../../src/interfaces/) can be a convenient way of avoiding unwanted direct dependencies or circular dependencies between libraries.

- *libquffen_consensus* should be a standalone dependency that any library can depend on, and it should not depend on any other libraries itself.

- *libquffen_util* should also be a standalone dependency that any library can depend on, and it should not depend on other internal libraries.

- *libquffen_common* should serve a similar function as *libquffen_util* and be a place for miscellaneous code used by various daemon, GUI, and CLI applications and libraries to live. It should not depend on anything other than *libquffen_util* and *libquffen_consensus*. The boundary between _util_ and _common_ is a little fuzzy but historically _util_ has been used for more generic, lower-level things like parsing hex, and _common_ has been used for quffen-specific, higher-level things like parsing base58. The difference between util and common is mostly important because *libquffen_kernel* is not supposed to depend on *libquffen_common*, only *libquffen_util*. In general, if it is ever unclear whether it is better to add code to *util* or *common*, it is probably better to add it to *common* unless it is very generically useful or useful particularly to include in the kernel.


- *libquffen_kernel* should only depend on *libquffen_util* and *libquffen_consensus*.

- The only thing that should depend on *libquffen_kernel* internally should be *libquffen_node*. GUI and wallet libraries *libquffenqt* and *libquffen_wallet* in particular should not depend on *libquffen_kernel* and the unneeded functionality it would pull in, like block validation. To the extent that GUI and wallet code need scripting and signing functionality, they should be get able it from *libquffen_consensus*, *libquffen_common*, and *libquffen_util*, instead of *libquffen_kernel*.

- GUI, node, and wallet code internal implementations should all be independent of each other, and the *libquffenqt*, *libquffen_node*, *libquffen_wallet* libraries should never reference each other's symbols. They should only call each other through [`src/interfaces/`](`../../src/interfaces/`) abstract interfaces.

## Work in progress

- Validation code is moving from *libquffen_node* to *libquffen_kernel* as part of [The libquffenkernel Project #24303](https://github.com/quffen/quffen/issues/24303)
- Source code organization is discussed in general in [Library source code organization #15732](https://github.com/quffen/quffen/issues/15732)
