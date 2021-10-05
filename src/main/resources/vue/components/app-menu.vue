<template id="menu">
    <!-- TODO: somehow get this outside of the main tag -->
    <header>
        <div class="navbar-fixed">
            <nav class="indigo">
                <div class="nav-wrapper container">
                    <a href="#" data-target="server-setting-tabs" class="sidenav-trigger"><i class="material-icons">menu</i></a>
                    <a href="/" class="brand-logo truncate">
                        TODO
                    </a>
                </div>
            </nav>

            <ul id="server-setting-tabs" class="discord not-black sidenav sidenav-fixed">
                <li class="bold active">
                    <a class="white-text" href="#basic">Basic Settings</a>
                </li>
                <li class="bold">
                    <a class="white-text" href="#moderation">Moderation Settings</a>
                </li>
                <li class="bold">
                    <a class="white-text" href="#welcome-leave">Welcome/Leave message</a>
                </li>
                <li class="divider grey darken-2"></li>
                <li class="bold">
                    <a class="white-text" href="#custom-commands">Custom Commands</a>
                </li>
            </ul>
        </div>
    </header>
</template>

<script>
    Vue.component('app-menu', {
        template: '#menu',
    });
</script>

<style>
    header, main, footer {
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
