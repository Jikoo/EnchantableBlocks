package com.github.jikoo.enchantableblocks.block;

import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemStackMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import com.jparams.verifier.tostring.ToStringVerifier;
import com.jparams.verifier.tostring.preset.Presets;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Feature: Enchantable blocks.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableBlockTest {

  private EnchantableRegistration registration;
  private Block block;
  private ItemStack itemStack;
  private ConfigurationSection storage;

  @BeforeAll
  void beforeAll() {
    var server = ServerMocks.mockServer();
    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);
  }

  @BeforeEach
  void beforeEach() {
    registration = mock(EnchantableRegistration.class);
    block = mock(Block.class);
    itemStack = mock(ItemStack.class);
    doAnswer(invocation -> itemStack).when(itemStack).clone();
    storage = mock(ConfigurationSection.class);
    when(storage.getItemStack(anyString())).thenReturn(itemStack);
  }

  @DisplayName("Enchantable blocks retrieve in-world block.")
  @Test
  void testGetBlock() {
    var enchantableBlock = new EnchantableBlock(registration, block, itemStack, storage) {};
    assertThat("Block is expected block", enchantableBlock.getBlock(), is(block));
  }

  @DisplayName("Constructor clones and singularizes creation item.")
  @Test
  void testGetItemStack() {
    ItemStack itemStackClone = mock(ItemStack.class);
    doAnswer(invocation -> itemStackClone).when(itemStack).clone();
    when(storage.getItemStack(anyString())).thenReturn(itemStackClone);
    when(itemStackClone.getAmount()).thenReturn(2);

    var enchantableBlock = new EnchantableBlock(registration, block, itemStack, storage) {};
    verify(itemStack).clone();
    ItemStack internalStack = enchantableBlock.getItemStack();
    assertThat("Item is clone", internalStack, CoreMatchers.is(itemStackClone));
    verify(itemStackClone).setAmount(1);
    // Directly returning the internal ItemStack instance allows subclasses to manipulate it.
    assertThat("Same item is returned", internalStack == enchantableBlock.getItemStack());
  }

  @DisplayName("Block checks against in-world type.")
  @Test
  void testIsCorrectBlockType() {
    var enchantableBlock = spy(new EnchantableBlock(registration, block, itemStack, storage) {});
    doReturn(true).when(enchantableBlock).isCorrectType(any());

    assertThat("Block is correct type", enchantableBlock.isCorrectBlockType());
    verify(block).getType();
    verify(enchantableBlock).isCorrectType(any());
  }

  @DisplayName("Block is tickable.")
  @Test
  void testTick() {
    var enchantableBlock = new EnchantableBlock(registration, block, itemStack, storage) {};
    assertDoesNotThrow(enchantableBlock::tick);
  }

  @DisplayName("Type is checked against registration listing.")
  @Test
  void testIsCorrectType() {
    var enchantableBlock = new EnchantableBlock(registration, block, itemStack, storage) {};

    Material material = Material.DIRT;
    doReturn(Set.of(), Set.of(material)).when(registration).getMaterials();
    assertThat(
        "Material is checked against current registration",
        enchantableBlock.isCorrectType(material),
        is(false));
    verify(registration).getMaterials();
    assertThat(
        "Material is checked against current registration",
        enchantableBlock.isCorrectType(material));
    verify(registration, times(2)).getMaterials();
  }

  @DisplayName("Changes to item must register as needing saving.")
  @Test
  void testDirtyStorage() {
    ItemStack itemStackClone = mock(ItemStack.class);
    doAnswer(invocation -> itemStackClone).when(itemStack).clone();

    var enchantableBlock = spy(new EnchantableBlock(registration, block, itemStack, storage) {});

    assertThat("EnchantableBlock is dirty", enchantableBlock.isDirty());
    verify(enchantableBlock).updateStorage();

    enchantableBlock.setDirty(false);

    assertThat("EnchantableBlock is dirty until saved", enchantableBlock.isDirty());
    verify(enchantableBlock, times(2)).updateStorage();

    doReturn(itemStackClone).when(storage).getItemStack("itemstack");
    enchantableBlock.setDirty(false);

    assertThat("EnchantableBlock is not dirty", enchantableBlock.isDirty(), is(false));
  }

  @DisplayName("Enchantable blocks retrieve provided storage.")
  @Test
  void testGetStorage() {
    var enchantableBlock = new EnchantableBlock(registration, block, itemStack, storage) {};
    assertThat("Storage is provided value", enchantableBlock.getStorage(), is(storage));
  }

  @DisplayName("Enchantable blocks retrieve provided registration.")
  @Test
  void testGetRegistration() {
    var enchantableBlock = new EnchantableBlock(registration, block, itemStack, storage) {};
    assertThat("Registration is provided value", enchantableBlock.getRegistration(), is(registration));
  }

  @DisplayName("Enchantable blocks retrieve config from registration.")
  @Test
  void testGetConfig() {
    var config = mock(EnchantableBlockConfig.class);
    doReturn(config).when(registration).getConfig();

    var enchantableBlock = new EnchantableBlock(registration, block, itemStack, storage) {};

    verify(registration, times(0)).getConfig();
    assertThat("Config is provided value", enchantableBlock.getConfig(), is(config));
    verify(registration).getConfig();
    assertThat("Config is not cached", enchantableBlock.getConfig(), is(config));
    verify(registration, times(2)).getConfig();
  }

  @DisplayName("Enchantable blocks should provide a descriptive toString.")
  @Test
  void testToString() {

    ToStringVerifier.forPackage(
        "com.github.jikoo.enchantableblocks.block",
        true,
        clazz -> {
          if (!EnchantableBlock.class.isAssignableFrom(clazz)) {
            return false;
          }
          Class<?> enclosingClass = clazz.getEnclosingClass();
          if (enclosingClass == null) {
            return true;
          }
          // Don't run on anything that is a subclass of a test.
          // Tests can easily be identified by JUnit annotations.
          return Arrays.stream(enclosingClass.getAnnotations())
              .map(Annotation::annotationType)
              .map(Class::getPackageName)
              .distinct()
              .noneMatch(packageName -> packageName.equals("org.junit.jupiter.api"));
        })
        .withPreset(Presets.INTELLI_J)
        .withIgnoredFields("registration", "storage", "dirty", "updating")
        .withValueProvider(ItemStack.class, path -> ItemStackMocks.newItemMock(ItemType.AIR, 1))
        .withFailOnExcludedFields(true).verify();
  }

}