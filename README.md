# Exclude Access

Simple plugin to exclude access from certain countries or IP addresses.

## Installation

1. Download the plugin from the [GitHub repository](https://github.com/hydrogend/excludeaccess).
2. Upload the plugin to the `plugins` directory.

## Configuration

The plugin can be configured in the `config.php` file. The following options are available:

- `license-key`: The license key for GeoLite2.
- `permit-countries`: An array of country codes that are allowed to login.
- `allowed-ips`: An array of IP addresses that are allowed to login.
- `check-only-allowed-ips`: If set to `true`, only the IP addresses in the `allowed-ips` array are allowed to login.
- `discord-webhook`: The Discord webhook URL to send notifications to.
- `download-url`: The URL to download the GeoLite2 database from.
- `temporarily-ban-threshold`: The number of failed login attempts before temporarily banning the user.
- `temporarily-ban-days`: The number of days to temporarily ban the user for.

## Permissions

`excludeaccess.command` - Allows the user to use the `/excludeaccess` command. Defaults to only operators.

## Commands

- `/excludeaccess reload` - Reloads the configuration file.

## License

This plugin is licensed under the GNU General Public License v3.0. You can view the license [here](LICENSE).
