# Source attribution and licensing

TinkersArmoryNirvana is a source port of **AntiqueArmory / Construct's Armory** to Minecraft 1.21.1 and NeoForge. It is a separate project, not an official C4 or Slime Knights release.

- Construct's Armory: Copyright (C) 2018–2020 C4 and contributors. Licensed under **LGPL-3.0-or-later**.
- AntiqueArmory source: <https://github.com/Elite-Modding-Team/AntiqueArmory>, reference commit `b94b199a93457511089092b5fc6f5a80be985903` supplied locally for this port.
- Port implementation: Copyright (C) 2026 GreedyCraftNirvanaMC contributors, **LGPL-3.0-or-later**.
- The original armor material values, trait/modifier behavior, Tabula armor/accessory geometry, textures, translations and book resources are adapted or retained from that source. Original resource names and the `conarm` content namespace are retained where applicable.
- TinkersNirvana supplies the material, material-input, smeltery, fluid and native workshop APIs. Its JAR is a separate required dependency and is not embedded in this distribution. See its own notices and license for its source and its TinkersAntique/Mantle adaptations.
- The supplied Mantle 1.12 source provides the original book/paper/cover/navigation textures and the 206×200 leaf layout used by the modern Armory manual. The section/text elements and navigation behavior are adapted from its book engine. Mantle is Copyright (c) 2013–2014 Slime Knights and licensed under MIT; the complete notice is retained in `licenses/Mantle-MIT.txt`. This project does not require the 1.12 Mantle JAR.
- TinkersAntique supplies the source behavior for `GuiInfoPanel`, `GuiOutlineTexture` / `ExtraUtilityTexture`, and the book listing/icon helpers. The original `textures/gui/book/modify.png` and `sounds/little_saw.ogg` are retained byte-for-byte under the `conarm` namespace. These portions retain the supplied TinkersAntique source's **LGPL-3.0-only** terms and Slime Knights/contributors attribution; its complete license is in `licenses/TinkersAntique-LGPL-3.0.txt`. Source reference: <https://github.com/Elite-Modding-Team/TinkersAntique>. The complete distribution includes LGPL-3.0-or-later Armory content, these LGPL-3.0-only portions and MIT Mantle portions; the root Armory grant is unchanged.
- The original MDK template license remains in `TEMPLATE_LICENSE.txt`.

`LICENSE` preserves the original license grant. Complete LGPLv3 and GPLv3 texts are provided in `licenses/`. Modified source is distributed in this repository and in the generated sources JAR. Retain these notices when redistributing or adapting this port and follow the LGPL's source and relinking requirements.

The separately supplied Minecraft-Mod-Language-Modpack uses CC BY-NC-SA 4.0. Its files are **not copied into this distribution**. The included legacy translations come from AntiqueArmory itself; new interface and documentation text is part of this port.

The project logo `tinkersarmorynirvana-logo.png` is an original AI-generated emblem for this port, distributed under LGPL-3.0-or-later. Its prompt and provenance are recorded in `docs/LOGO.md`; it is not an upstream mod logo.
