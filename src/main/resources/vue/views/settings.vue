<template id="base-settings">
    <div>
        <div class="container">
            Settings
            <div v-if="settingData.loaded">
                {{ settings }}
            </div>
        </div>
    </div>
</template>

<script>
    Vue.component('settings', {
        template: '#base-settings',
        data () {
            const guildId = this.$javalin.pathParams['guildId'];

            return {
                settingData: new LoadableData(`/api/guilds/${guildId}/settings`, false),
            };
        },
        computed: {
            settings () {
                return this.settingData.data.settings;
            },
            roles () {
                return this.settingData.data.roles;
            },
            channels () {
                return this.settingData.data.channels;
            },
            guild () {
                return this.settingData.data.guild;
            },
        },
    });
</script>
