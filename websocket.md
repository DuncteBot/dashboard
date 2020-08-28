# websocket ideas

- fetch jda entities over WS
- check permissions
- check if bot in guild
- creating role hashes

#### Internal data exchange
Request:
```json
{
  "roles_put_hash": {
    "guild_id": "1321513153",
    "hash": "blablabla"
  }
}
```
Response:
```json
{
  "roles_put_hash": {
    "guild_id": "1321513153",
    "hash": "blablabla",
    "success": true 
  }
}
```

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

#### getting command info
Request:
```json
{
  "retrieve": {
    "commands": {}
  }
}
```
Response:
```json
{
  "commands": [
    {
      "name": "etc..."
    }
  ]
}
```

#### getting guild info
All fields are optional

Request:
```json
{
  "retrieve": {
    "guilds": [
      "list of guild ids"
    ],
    "guild_member_info": {
      "guild_id": "13456",
      "member_id": "5464654684"
    }
  }
}
```
Response:
```json
{
  "guilds": [
    {
      "guild_id": "13456",
      "member_count": 999, // or -1 for not in server
      "text_channels": [
        // JDA text channel object
      ],
      "voice_channels": [
        // JDA voice channel object
      ],
      "roles": [
        // JDA role object
      ]
    }
  ],
   "guild_member_info": {
      "guild_id": "13456",
      "member_id": "5464654684",
      "member": {
        // JDA member object
      }
   }
}
```
