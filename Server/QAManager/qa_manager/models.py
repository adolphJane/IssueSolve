# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models

# Create your models here.

class QA_API(models.Model):
    issue = models.TextField(null=True,default=None,blank=True)
