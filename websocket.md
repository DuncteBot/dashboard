# websocket ideas

- fetch jda entities over WS
- check permissions
- check if bot in guild

#### invalidating guild settings and updating
```json
{
  "guild_settings": {
    "remove": [
      "array of guild ids to forget the settings for"
    ],
    "update": [
      "array of guild ids to update the settings for"
    ],
    "add": [
      "array of guild ids to fetch the settings for"
    ]
  }
}
```
