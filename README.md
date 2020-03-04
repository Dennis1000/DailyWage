# DailyWage - Your daily wage

A Spigot/Bukkit plugin command that pays out a daily wage (needs the great [DKCoins](https://github.com/DevKrieger/DKCoins) plugin!).

For discussion visit

​	https://www.spigotmc.org/resources/daily-wage.75728/


## Installation 

 1. Install and configure [DKCoins](https://github.com/DevKrieger/DKCoins)
 2. Download the current DailyWage version from SpigotMC
 3. Put the plugin `DailyWage.jar` in your plugin folder (ignore any other files in the `.zip` file!).
 4. Restart your server.

## Configuration

After running the plugin on your server a `config.yml` file is created in the plugin folder. Adjust these settings to your needs:

- `CoinsPerDay:` the amount of coins payed as the daily wage (default `1`)
- `Password:` set a password for the command, use digits 0-9 and the letters a/A to z/Z only (default `empty`)
- `BackPayment Enable:` set it to `true` to back pay the wage since last pay out (default `true`)
- `BackPayment MaxDays:` maximum days for back payment since last pay out (default `100`)

*Restart your server after changing any of those values*!

## Usage

`/wage` (or `/lohn` or `/salaire`  - these are command aliases for german and french).

Run this command (without any parameters) to receive your daily wage. If run more than once a day an error message is issued.

If there is a password set in the config (see Configuration above), you need to attach the password

`/wage mysecretpassword`

You may pay out the wage to a named person by adding the target player name with

`/wage playername`

or if a password is set then use

`/wage mysecretpassword playername`

In order to use it in command block you may state `@p` as the player name in order to pay out to the nearest player

`/wage mysecretpassword @p`

## Localization

After running the plugin on your server a `messages.yml` file is created in the plugin folder.  Feel free to adapt those messages to your needs. The `messages.yml` file uses short color codes (eg. `&c` for red), see [Color Codes](https://github.com/Bukkit/Bukkit/blob/master/src/main/java/org/bukkit/ChatColor.java).

There is a german translated message file called `messages-german.yml` in the zip file. Rename it to `messages.yml` and replace the above mentioned `messages.yml` on the server.

*Restart your server after changing any of those messages!*


## License

This project is licensed under the Apache License - see the [LICENSE](LICENSE) file for more information.
