# -*- coding: utf-8 -*-
require 'spec_helper'
require 'candlepin_scenarios'
require 'rexml/document'

describe 'Consumer Dev Resource' do

  include CandlepinMethods
  include AttributeHelper

  before(:each) do
    skip("candlepin running in standalone mode") if not is_hosted?

    @owner = create_owner random_string('test_owner')
    @username = random_string("user")
    @consumername = random_string("dev_consumer")
    @user = user_client(@owner, @username)

    # active subscription to allow this all to work
    active_product = create_product
    active_subscription = @cp.create_pool(@owner['key'], active_product['id'], {
      :subscription_id => random_str('source_sub'),
      :upstream_pool_id => random_str('upstream'),
      :quantity => 10
    })

    @provided_product_1 = create_upstream_product("prov_product_1", { :name => "provided product 1" })
    @provided_product_2 = create_upstream_product("prov_product_2", { :name => "provided product 2" })

    @dev_product_1 = create_upstream_product("dev_product_1", {
        :name => "dev product 1",
        :attributes => {
            :expires_after => "60"
        },
        :providedProducts => [@provided_product_1, @provided_product_2]
    })

    @dev_product_2 = create_upstream_product("dev_product_2", {
        :name => "dev product 2",
        :attributes => {
            :expires_after => "60"
        },
        :providedProducts => [@provided_product_1, @provided_product_2]
    })

    @consumer = consumer_client(@user, @consumername, :system, 'dev_user', facts = {
      :dev_sku => @dev_product_1['id']
    })

    @consumer.update_consumer({:installedProducts => [
      {'productId' => @provided_product_1['id'], 'productName' => @provided_product_1['name']},
      {'productId' => @provided_product_2['id'], 'productName' => @provided_product_2['name']}
    ]})
  end

  it 'should create entitlement to newly created dev pool' do
    auto_attach_and_verify_dev_product(@dev_product_1['id'])
  end

  it 'should create new entitlement when dev pool already exists' do
    initial_ent = auto_attach_and_verify_dev_product(@dev_product_1['id'])
    auto_attach_and_verify_dev_product(@dev_product_1['id'], initial_ent.id)
  end

  it 'should create new entitlement when dev_sku attribute changes' do
    ent = auto_attach_and_verify_dev_product(@dev_product_1['id'])
    @consumer.update_consumer({:facts => { :dev_sku => @dev_product_2['id'] }})
    auto_attach_and_verify_dev_product(@dev_product_2['id'], ent.id)
  end

  def auto_attach_and_verify_dev_product(expected_product_id, old_ent_id=nil)
    @consumer.consume_product()
    entitlements = @consumer.list_entitlements()
    entitlements.length.should eq(1)
    unless old_ent_id.nil?
      entitlements[0].id.should_not eq(old_ent_id)
    end

    new_pool = entitlements[0].pool
    new_pool.type.should eq("DEVELOPMENT")
    new_pool.productId.should eq(expected_product_id)
    new_pool.providedProducts.length.should eq(2)
    return entitlements[0]
  end

  it 'should allow sub-man-gui process for auto-bind' do
    pools = @consumer.autobind_dryrun()
    expect(pools.length).to eq(1)
    ents = @consumer.consume_pool(pools[0].pool.id)
    ents.length.should eq(1)
    ent_pool = ents[0].pool
    ent_pool.type.should eq("DEVELOPMENT")
    ent_pool.productId.should eq(@dev_product_1['id'])
    ent_pool.providedProducts.length.should eq(2)
    ent_pool.id.should eq(pools[0].pool.id)
  end

  it 'should not allow entitlement for consumer past expiration' do
    created_date = '2015-05-09T13:23:55+0000'
    consumer = @user.register(random_string('system'), type=:system, nil, facts = {:dev_sku => @dev_product_1['id']},
      @username, @owner['key'], [], [], [], [], nil, [], created_date)
    consumer_client = Candlepin.new(nil, nil, consumer['idCert']['cert'], consumer['idCert']['key'])
    expected_error = "Unable to attach subscription for the product \"#{@dev_product_1['id']}\": Subscriptions for #{@dev_product_1['id']} expired on:"

    begin
      consumer_client.consume_product()
      fail("Expected Forbidden since consumer is older than product expiry")
    rescue RestClient::Forbidden => e
      message = JSON.parse(e.http_body)['displayMessage']
      message.should start_with(expected_error)
    end
  end

  it 'should update dev products on refresh' do
    auto_attach_and_verify_dev_product(@dev_product_1['id'])

    provided_product_3 = create_upstream_product("prov_product_3", { :name => "provided product 3" })

    upstream_dev_product = update_upstream_product("dev_product_1", {
        :name => "updated dev product 1",
        :attributes => {
            :test_attrib => "test_value",
            :expires_after => "90"
        },
        :providedProducts => [@provided_product_1, @provided_product_2, provided_product_3]
    })

    existing_dev_product = @cp.get_product(@owner['key'], @dev_product_1['id'])

    @cp.refresh_pools(@owner['key'])

    updated_dev_product = @cp.get_product(@owner['key'], @dev_product_1['id'])

    # Verify base state
    expect(existing_dev_product['id']).to eq(@dev_product_1['id'])
    expect(existing_dev_product['name']).to eq(@dev_product_1['name'])
    expect(get_attribute_value(existing_dev_product['attributes'], 'expires_after')).to eq(get_attribute_value(@dev_product_1['attributes'], 'expires_after'))
    expect(get_attribute_value(existing_dev_product['attributes'], 'test_attrib')).to be_nil

    # Verify updated state
    expect(updated_dev_product['id']).to eq(@dev_product_1['id'])
    expect(updated_dev_product['name']).to_not eq(@dev_product_1['name'])
    expect(get_attribute_value(updated_dev_product['attributes'], 'expires_after')).to_not eq(get_attribute_value(@dev_product_1['attributes'], 'expires_after'))
    expect(get_attribute_value(updated_dev_product['attributes'], 'test_attrib')).to_not be_nil

    expect(updated_dev_product['name']).to eq(upstream_dev_product['name'])
    expect(get_attribute_value(updated_dev_product['attributes'], 'expires_after')).to eq(get_attribute_value(upstream_dev_product['attributes'], 'expires_after'))
    expect(get_attribute_value(updated_dev_product['attributes'], 'test_attrib')).to eq(get_attribute_value(upstream_dev_product['attributes'], 'test_attrib'))
  end

end
