# websocket ideas

- fetch jda entities over WS
- check permissions
- check if bot in guild
- creating role hashes

# todo
- jda to json
- shared code packages
- flat repository (all packages in a single repository)

#### Authorising
Send an "Authorization" header with the correct token (one that is for the bot routes)


#### Internal data exchange
Request (from-bot):
```json
{
  "t": "ROLES_PUT_HASH",
  "d": {
    "guild_id": "1321513153",
    "hash": "blablabla"
  }
}
```
Response (to-bot):
```json
{
  "t": "ROLES_PUT_HASH",
  "data": {
    "guild_id": "1321513153",
    "hash": "blablabla",
    "success": true 
  }
}
```

#### data updates
Request (to-bot):

TODO: also do guild settings in here? 
```json
{
  "t": "DATA_UPDATE",
  "d": {
    "patrons": {
      "add": [],
      "remove": []
    },
    "unbans": [],
    "unmutes": [],
    "reminders": []
  }
}
```

#### invalidating guild settings and updating
Request (to-bot):

both update and add send the full guild settings object to the bot
```json
{
  "t": "GUILD_SETTINGS",
  "d":{
    "remove": [
      "array of guild ids to forget the settings for"
    ],
    "update": [
      {
        "guildId": "guildid"
      }
    ],
    "add": [
      {
        "guildId": "guildid"
      }
    ]
  } 
}
```
```json
{
  "t": "CUSTOM_COMMANDS",
  "d": {
    "remove": [
        {
          "name": "a_name",
          "guild_id": "16515631"
        }
    ],
    "update": [
        {
          "name": "a_name",
          "guild_id": "16515631",
          "content": "This is a cool command {atuser} 123"
        }
    ],
    "add": [
      {
        "name": "a_name",
        "guild_id": "16515631",
        "content": "This is a cool command {atuser}"
      }
    ]
  }
}
```
Response (from-bot): None

#### getting command info
Request (to-bot):
```json
{
  "t": "RETRIEVE",
  "identifier": "Identifier for finding the data we requested",
  "commands": {}
}
```
Response (from-bot):
```json
{
  "t": "RETRIEVE",
  "identifier": "Identifier for finding the data we requested",
  "commands": [
    {
      "name": "etc..."
    }
  ]
}
```

#### getting guild info
All data fields are optional

Request (to-bot):
```json
{
  "t": "FETCH_DATA",
  "d": {
    "identifier": "Identifier for finding the data we requested",
    "guilds": [
      "list of guild ids"
    ],
    "guild_member_info": [
      {
        "guild_id": "13456",
        "member_id": "5464654684"
      }
    ]
  }
}
```
Response (from-bot):
```json
{
  "t": "FETCH_DATA",
  "d": {
    "identifier": "Identifier for finding the data we requested",
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
    "guild_member_info": [
       {
        "guild_id": "13456",
        "member_id": "5464654684",
        "member": {
           // JDA member object
        }
       }
    ]
  }
}
```
