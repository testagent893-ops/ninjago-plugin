# Ninjago SMP - Plugin Paper 1.21.11 (V1.1.0)

## Armes
- `sword_of_fire` — Netherite Sword — Custom Model Data 1
- `shurikens_of_ice` — Bow — Custom Model Data 2
- `nunchucks_of_lightning` — Netherite Sword — Custom Model Data 3
- `scythe_of_quakes` — Netherite Axe — Custom Model Data 4

## Commandes
- `/ninjago give <joueur> <arme>`
- `/ninjago list`
- `/ninjago remove <joueur> <arme>
- `/ninjago reload``

## Pouvoirs V1
- Épée de Feu : clic droit, projectile de feu.
- Shurikens de Glace : tir de 3 projectiles glacés.
- Nunchakus : décharge sur jusqu'à 3 ennemis proches.
- Faux : onde de choc avec dégâts et repoussement.

## Build
Java 21 et Paper 1.21.11.

```bash
mvn package
```

Le JAR sera généré dans `target/ninjago-smp-1.1.0.jar`.

## Installation Aternos
Aternos ne permet pas l'upload direct d'un plugin personnalisé. Le JAR doit être publié sur une plateforme prise en charge puis éventuellement suggéré à Aternos.
