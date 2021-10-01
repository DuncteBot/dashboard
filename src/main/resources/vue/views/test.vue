<template id="thread-view">
    <div>
        <p>{{ $javalin.pathParams["user"] }}</p>
        <section>
            <input type="text" v-model="user">
            <a :href="`/vue/${user}`">Update</a>
        </section>
    </div>
</template>
<script>
    Vue.component("thread-view", { // the component name is what we need to enter in VueComponent("...")
        template: "#thread-view",
        data () {
            return {
                user: this.$javalin.pathParams["user"],
            };
        },
    });
</script>
