<template>
  <q-layout view="hHh Lpr lFf"
    style="height: 100vh">
    <q-header elevated>
      <q-toolbar>
        <q-btn
          flat
          dense
          round
          icon="menu"
          aria-label="Menu"
          @click="toggleDrawer"
        />

        <q-toolbar-title>
          Map Service
        </q-toolbar-title>

        <div>Quasar v{{ $q.version }}</div>
      </q-toolbar>
    </q-header>

    <q-drawer
        v-model="drawer"
        show-if-above
        :mini="!drawer || mini"
        bordered
        style="display: flex; flex-direction: column;"
    >
      <q-list style="flex-grow: 1">
        <EssentialLink 
          v-for="link in essentialLinks"
          :key="link.title"
          v-bind="link"
        />
      </q-list>

      <q-list>
        <q-separator />
        <q-item 
          clickable
          @click="toggleMini"
        >

          <q-item-section
            avatar
          >
            <q-icon :name="expandIcon" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Collapse</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container
      style="height: 100%">
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import EssentialLink, { EssentialLinkProps } from 'components/EssentialLink.vue';

const essentialLinks: EssentialLinkProps[] = [
  {
    title: 'Add',
    icon: 'add',
  },
  {
    title: 'Docs',
    icon: 'school',
  },
  {
    title: 'Github',
    icon: 'code',
  },
  {
    title: 'Discord Chat Channel',
    icon: 'chat',
  },
  {
    title: 'Forum',
    icon: 'record_voice_over',
  },
  {
    title: 'Twitter',
    icon: 'rss_feed',
  },
  {
    title: 'Facebook',
    icon: 'public',
  },
  {
    title: 'Quasar Awesome',
    icon: 'favorite',
  }
];

const drawer = ref(false)
function toggleDrawer() {
  drawer.value = !drawer.value
}

const mini = ref(true);
const expandIcon = ref('chevron_right');
function toggleMini() {
  mini.value = !mini.value;
  expandIcon.value = (mini.value) ? 'chevron_right' : 'chevron_left';
}

</script>
