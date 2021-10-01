<template id="thread-view">
    <div>{{ $javalin.pathParams["user"] }}</div>
</template>
<script>
    Vue.component("thread-view", { // the component name is what we need to enter in VueComponent("...")
        template: "#thread-view"
    });
</script>
