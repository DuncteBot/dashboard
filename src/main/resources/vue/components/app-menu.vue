<template id="app-menu">
    <header>
        <div class="navbar-fixed">
            <nav class="indigo">
                <div class="nav-wrapper container">
                    <a href="#" data-target="server-setting-tabs" class="sidenav-trigger"><i class="material-icons">menu</i></a>
                    <a v-if="guildName" href="/" class="brand-logo truncate">
                        <i class="material-icons">arrow_back</i>  Editing {{ guildName }}
                    </a>
                </div>
            </nav>

            <ul id="server-setting-tabs" class="discord not-black sidenav sidenav-fixed">
                <li class="bold" :class="getActiveClass('basic')">
                    <a class="white-text"
                       href="#basic"
                       @click.prevent="$emit('change-menu', 'basic')">Basic Settings</a>
                </li>
                <li class="bold" :class="getActiveClass('moderation')">
                    <a class="white-text"
                       href="#moderation"
                       @click.prevent="$emit('change-menu', 'moderation')">Moderation Settings</a>
                </li>
                <li class="bold" :class="getActiveClass('welcome-leave')">
                    <a class="white-text"
                       href="#welcome-leave"
                       @click.prevent="$emit('change-menu', 'welcome-leave')">Welcome/Leave message</a>
                </li>
                <li class="divider grey darken-2"></li>
                <li class="bold" :class="getActiveClass('custom-commands')">
                    <a class="white-text"
                       href="#custom-commands"
                       @click.prevent="$emit('change-menu', 'custom-commands')">Custom Commands</a>
                </li>
            </ul>
        </div>
    </header>
</template>

<script>
    Vue.component('app-menu', {
        template: '#app-menu',
        props: {
            sidenav: Boolean,
            guildName: {
                type: String,
                default: null,
            },
            showing: {
                type: String,
                default: null,
            },
        },
        methods: {
            getActiveClass (name) {
                return {
                    active: this.showing === name,
                };
            },
        },
    });
</script>

<style>
    main, footer {
        padding-left: 300px;
    }

    @media only screen and (max-width : 992px) {
        header, main, footer {
            padding-left: 0;
        }
    }

    .sidenav .sidenav-fixed {
        top: 20px;
    }
</style>
